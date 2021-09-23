/*
 * Copyright 2021 ByteLegend Technologies and the original author or authors.
 *
 * Licensed under the GNU Affero General Public License v3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://github.com/ByteLegend/ByteLegend/blob/master/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.bytelegend.client.app.external

import com.bytelegend.app.client.misc.githubUrlToRawGithubUserContentUrl
import com.bytelegend.app.client.ui.bootstrap.BootstrapSpinner
import com.bytelegend.client.app.ui.GameProps
import com.bytelegend.client.app.web.checkStatusCode
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import react.RBuilder
import react.RComponent
import react.State
import react.setState

interface LoadableMarkdownProps : GameProps {
    var link: String
    var allowRawHtml: Boolean
}

interface LoadableMarkdownState : State {
    var markdownContent: String?
}

class LoadableMarkdown : RComponent<LoadableMarkdownProps, LoadableMarkdownState>() {
    private var loading = false
    override fun LoadableMarkdownState.init() {
        markdownContent = null
    }

    @Suppress("SimplifyBooleanWithConstants")
    override fun RBuilder.render() {
        if (state.markdownContent == null) {
            BootstrapSpinner {
                attrs.animation = "border"
            }
            if (!loading) {
                GlobalScope.launch {
                    loading = true
                    val content = window.fetch(rebuildUrl(props.link))
                        .await()
                        .checkStatusCode()
                        .text()
                        .await()
                    loading = false
                    setState {
                        this.markdownContent = content
                    }
                }
            }
        } else {
            ReactMarkdown {
                +state.markdownContent!!
                attrs.transformImageUri = { src: String, _: Any, _: Any ->
                    rebuildUrl(src)
                }
                if (props.allowRawHtml == true) {
                    attrs.rehypePlugins = arrayOf(RehypeRaw)
                }
            }
        }
    }

    /**
     * GitHub doesn't support iframe, so we have to use raw.githubusercontent.com
     *
     */
    private fun rebuildUrl(url: String): String {
        val replaceToRawGithubUserContent = githubUrlToRawGithubUserContentUrl(url)

        return if (props.game.gfw) {
            replaceToRawGithubUserContent.replace("https://raw.githubusercontent.com/", "/ghraw/")
        } else {
            replaceToRawGithubUserContent
        }
    }

    override fun UNSAFE_componentWillReceiveProps(nextProps: LoadableMarkdownProps) {
        if (nextProps.link != props.link) {
            loading = false
            setState {
                markdownContent = null
            }
        }
    }
}
