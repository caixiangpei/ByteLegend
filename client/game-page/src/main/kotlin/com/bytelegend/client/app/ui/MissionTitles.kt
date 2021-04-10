package com.bytelegend.client.app.ui

import com.bytelegend.app.client.api.EventBus
import com.bytelegend.app.client.api.EventListener
import com.bytelegend.app.shared.GridCoordinate
import com.bytelegend.app.shared.PixelBlock
import com.bytelegend.app.shared.math.outOfCanvas
import com.bytelegend.app.shared.objects.GameMapMission
import com.bytelegend.app.shared.objects.GameObjectRole
import com.bytelegend.client.app.engine.GAME_CLOCK_50HZ_EVENT
import com.bytelegend.client.app.engine.MOUSE_MOVE_EVENT
import com.bytelegend.client.app.engine.MOUSE_OUT_OF_MAP_EVENT
import com.bytelegend.client.app.engine.MouseEventListener
import kotlinx.html.js.onMouseMoveFunction
import kotlinx.html.js.onMouseOutFunction
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.jsStyle
import react.dom.span
import react.setState

interface MissionTitlesProps : GameProps
interface MissionTitlesState : RState {
    var counter: Int
}

// Used to calculate whether the widget is inside the canvas, it doesn't have to be accurate.
const val ESTIMATE_WIDTH = 100
const val TITLE_HEIGHT = 32

class MissionTitles : GameUIComponent<MissionTitlesProps, MissionTitlesState>() {
    private val on50HzClockListener: EventListener<Nothing> = this::on50HzClock

    // counter increment on 50Hz clock
    override fun MissionTitlesState.init() {
        counter = 0
    }

    private fun on50HzClock(n: Nothing) {
        setState { counter += 1 }
    }

    override fun RBuilder.render() {
        activeScene.objects.getByRole<GameMapMission>(GameObjectRole.Mission)
            .filter { insideCanvas(it) && it.title.isNotBlank() }
            .forEach {
                child(MissionTile::class) {
                    val coordinateInGameContainer = calculateCoordinateInCanvas(it.point).coordinate + canvasCoordinateInGameContainer
                    attrs.eventBus = game.eventBus
                    attrs.left = coordinateInGameContainer.x + 2 // maybe border?
                    attrs.bottom = gameContainerHeight - coordinateInGameContainer.y + 32
                    attrs.offsetY = if (state.counter % 20 < 10) 0 else -2
                    attrs.title = i(it.title)
                    attrs.tileCoordinate = it.point
                }
            }
    }

    private fun insideCanvas(mission: GameMapMission): Boolean {
        return !calculateCoordinateInCanvas(mission.point).outOfCanvas(activeScene.canvasState.getCanvasPixelSize())
    }

    private fun calculateCoordinateInCanvas(point: GridCoordinate): PixelBlock {
        val tileSize = activeScene.map.tileSize
        val targetTilePixelCoordinate = point * tileSize
        val widgetLeftTopCornerXInCanvas = targetTilePixelCoordinate.x + tileSize.width / 2 - canvasCoordinateInMap.x
        val widgetLeftTopCornerYInCanvas = targetTilePixelCoordinate.y - canvasCoordinateInMap.y
        return PixelBlock(
            widgetLeftTopCornerXInCanvas, widgetLeftTopCornerYInCanvas, ESTIMATE_WIDTH, TITLE_HEIGHT
        )
    }

    override fun componentDidMount() {
        super.componentDidMount()
        props.game.eventBus.on(GAME_CLOCK_50HZ_EVENT, on50HzClockListener)
    }

    override fun componentWillUnmount() {
        super.componentWillUnmount()
        props.game.eventBus.remove(GAME_CLOCK_50HZ_EVENT, on50HzClockListener)
    }
}

interface CheckpointTitleProps : RProps {
    // coordinate in game container
    var left: Int
    var bottom: Int

    // for animation
    var offsetY: Int
    var title: String
    var tileCoordinate: GridCoordinate
    var eventBus: EventBus
}

interface CheckPointTitleState : RState {
    var hovered: Boolean
}

class MissionTile : RComponent<CheckpointTitleProps, CheckPointTitleState>() {
    private val mouseMoveListener: MouseEventListener = {
        if (it.mapCoordinate == props.tileCoordinate) {
            setState { hovered = true }
        } else {
            setState { hovered = false }
        }
    }
    private val mouseOutOfMapListener: EventListener<Any> = {
        setState {
            hovered = false
        }
    }

    override fun CheckPointTitleState.init() {
        hovered = false
    }

    private fun getOffsetY() = if (state.hovered) 0 else props.offsetY

    override fun RBuilder.render() {
        absoluteDiv(
            left = props.left,
            bottom = props.bottom + getOffsetY(),
            zIndex = Layer.CheckpointTitle.zIndex(),
            classes = setOf("checkpoint-title")
        ) {
            span {
                consumer.onTagContentUnsafe {
                    +props.title
                }
            }
            attrs.onMouseOutFunction = {
                setState { hovered = false }
            }
            attrs.onMouseMoveFunction = {
                setState { hovered = true }
            }
            if (state.hovered) {
                attrs.jsStyle {
                    boxShadow = "0 0 20px white"
                }
            }
            absoluteDiv(
                zIndex = Layer.CheckpointTitle.zIndex(),
                classes = setOf("checkpoint-title-bottom-border", "checkpoint-title-bottom-border-left")
            )
            absoluteDiv(
                zIndex = Layer.CheckpointTitle.zIndex(),
                classes = setOf("checkpoint-title-bottom-border", "checkpoint-title-bottom-border-right")
            )
        }

        absoluteDiv(
            left = props.left,
            bottom = props.bottom + getOffsetY() - 16,
            zIndex = Layer.CheckpointTitle.zIndex() + 2,
            classes = setOf("checkpoint-title-triangle-container")
        ) {
            absoluteDiv(
                left = 0,
                top = 0,
                width = 0,
                height = 0,
                classes = setOf("checkpoint-title-triangle")
            )
        }
    }

    override fun shouldComponentUpdate(nextProps: CheckpointTitleProps, nextState: CheckPointTitleState): Boolean {
        return props.left != nextProps.left ||
            props.bottom != nextProps.bottom ||
            props.title != nextProps.title ||
            props.offsetY != nextProps.offsetY ||
            state.hovered != nextState.hovered
    }

    override fun componentDidMount() {
        props.eventBus.on(MOUSE_MOVE_EVENT, mouseMoveListener)
        props.eventBus.on(MOUSE_OUT_OF_MAP_EVENT, mouseOutOfMapListener)
    }

    override fun componentWillUnmount() {
        props.eventBus.remove(MOUSE_MOVE_EVENT, mouseMoveListener)
        props.eventBus.remove(MOUSE_OUT_OF_MAP_EVENT, mouseOutOfMapListener)
    }
}