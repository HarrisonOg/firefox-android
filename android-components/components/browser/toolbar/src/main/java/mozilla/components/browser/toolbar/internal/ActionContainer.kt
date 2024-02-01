/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.toolbar.internal

import android.content.Context
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import mozilla.components.browser.toolbar.R
import mozilla.components.concept.toolbar.Toolbar

/**
 * A container [View] for displaying [Toolbar.Action] objects.
 */
internal class ActionContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private val actions = mutableListOf<ActionWrapper>()
    private var actionSize: Int? = null
    private var hasEndBoundActions: Boolean = false
    private var endBoundActionCount: Int = 0
    private var endBoundIndex: Int = -1  //Tracks the index before the end-bound items.

    init {
        gravity = Gravity.CENTER_VERTICAL
        orientation = HORIZONTAL
        visibility = View.GONE

        context.obtainStyledAttributes(
            attrs,
            R.styleable.ActionContainer,
            defStyleAttr,
            0,
        ).run {
            actionSize = attrs?.let {
                getDimensionPixelSize(R.styleable.ActionContainer_actionContainerItemSize, 0)
            }

            recycle()
        }
    }

    /**
     * Adds an action to the end of the list of actions unless [hasEndBoundActions] is true.
     * If [hasEndBoundActions], then adds the action at the index right before the end-bound items.
     */
    fun addAction(action: Toolbar.Action) {
        val wrapper = ActionWrapper(action)

        if (action.visible()) {
            visibility = View.VISIBLE

            action.createView(this).let {
                wrapper.view = it
                addActionView(it)
            }
        }

        if (endBoundIndex >= 0) {
            actions.add(endBoundIndex, item)
            endBoundIndex++
        } else {
            actions.add(wrapper)
        }
    }

    /**
     *
     */
    fun removeAction(action: Toolbar.Action) {
        actions.find { it.actual == action }?.let {
            if (endBoundIndex >= 0) {  //There are end-bound items to deal with
                if (actions.indexOf(it) < endBoundIndex ) {
                    //removing normal item
                    endBoundIndex--
                } else if(actions.indexOf(it) == endBoundIndex && (actions.size - 1) == endBoundIndex) {
                    //removing last end-bound item
                    //reset the endbound index to -1
                    endBoundIndex = -1
                } else {
                    //no-op
                    //removing one of multiple end-bound items
                    //no movement of endBoundIndex is needed
                }
            }
            actions.remove(it)
            removeView(it.view)
        }
    }

    fun invalidateActions() {
        TransitionManager.beginDelayedTransition(this)

        var updatedVisibility = View.GONE

        for (action in actions) {
            val visible = action.actual.visible()

            if (visible) {
                updatedVisibility = View.VISIBLE
            }

            if (!visible && action.view != null) {
                // Action should not be visible anymore. Remove view.
                removeView(action.view)
                action.view = null
            } else if (visible && action.view == null) {
                // Action should be visible. Add view for it.
                action.actual.createView(this).let {
                    action.view = it
                    addActionView(it)
                }
            }

            action.view?.let { action.actual.bind(it) }
        }

        visibility = updatedVisibility
    }

    fun autoHideAction(isVisible: Boolean) {
        for (action in actions) {
            if (action.actual.autoHide()) {
                action.view?.isVisible = isVisible
            }
        }
    }

    private fun addActionView(view: View) {
        addView(view, LayoutParams(actionSize ?: 0, actionSize ?: 0))
    }

    /**
     * Set this [ActionContainer] to have [ActionWrapper]s that are bound to the end
     * of the container (end-bound items) and the count of [ActionWrapper]s bound to the end.
     *
     * @param hasEndBound [Boolean] flag showing if the container has end-bound items.
     * @param endBoundCount [Int] num of end-bound items.
     */
    fun setEndBoundLimit(hasEndBound: Boolean, endBoundCount: Int,) {
       hasEndBoundActions = hasEndBound
       endBoundActionCount = endBoundCount
    }

    /**
     * Adds end-bound item to this container at the end of the list.
      */
    fun addEndBoundItem(action: Toolbar.Action) {
        val wrapper = ActionWrapper(action)

        if (action.visible()) {
            visibility = View.VISIBLE

            action.createView(this).let {
                wrapper.view = it
                addActionView(it)
            }
        }

        //new endBoundItems are added at the end of the list
        actions.add(wrapper)
        if (endBoundIndex < 0) {
            //if there are no end-bound items, this is the first one add the index.
            endBoundIndex = actions.indexOf(wrapper)
        }
    }
}
