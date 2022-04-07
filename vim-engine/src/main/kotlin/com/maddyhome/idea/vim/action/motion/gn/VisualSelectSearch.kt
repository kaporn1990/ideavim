/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim.action.motion.gn

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.noneOfEnum
import java.util.*
import kotlin.math.max

class VisualSelectNextSearch : MotionActionHandler.SingleExecution() {
  override val flags: EnumSet<CommandFlags> = noneOfEnum()

  override fun getOffset(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return selectNextSearch(editor, operatorArguments.count1, true).toMotionOrError()
  }

  override val motionType: MotionType = MotionType.EXCLUSIVE
}

class VisualSelectPreviousSearch : MotionActionHandler.SingleExecution() {
  override val flags: EnumSet<CommandFlags> = noneOfEnum()

  override fun getOffset(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return selectNextSearch(editor, operatorArguments.count1, false).toMotionOrError()
  }

  override val motionType: MotionType = MotionType.EXCLUSIVE
}

private fun selectNextSearch(editor: VimEditor, count: Int, forwards: Boolean): Int {
  val caret = editor.primaryCaret()
  val range = injector.searchGroup.getNextSearchRange(editor, count, forwards) ?: return -1
  val adj = injector.visualMotionGroup.selectionAdj
  if (!editor.inVisualMode) {
    val startOffset = if (forwards) range.startOffset else max(range.endOffset - adj, 0)
    caret.moveToOffset(startOffset)
    injector.visualMotionGroup.enterVisualMode(editor, CommandState.SubMode.VISUAL_CHARACTER)
  }
  return if (forwards) max(range.endOffset - adj, 0) else range.startOffset
}
