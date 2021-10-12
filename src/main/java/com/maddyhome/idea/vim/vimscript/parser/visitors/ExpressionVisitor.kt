package com.maddyhome.idea.vim.vimscript.parser.visitors

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.BinExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.DictionaryExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.EnvVariableExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.FalsyExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.FuncrefCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.FunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.LambdaExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.LambdaFunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.ListExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.OneElementSublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.OptionExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Register
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.TernaryExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.UnaryExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.BinaryOperator
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.UnaryOperator
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptBaseVisitor
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.BlobExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.DictionaryExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.EnvVariableExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.FalsyExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.FloatExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.FunctionCallExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.IntExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.ListExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.LiteralDictionaryExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.OneElementSublistExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.OptionExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.RegisterExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.StringExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.SublistExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.TernaryExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.UnaryExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.VariableContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.VariableExpressionContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.WrappedExpressionContext
import org.antlr.v4.runtime.ParserRuleContext

object ExpressionVisitor : VimscriptBaseVisitor<Expression>() {

  override fun visitDictionaryExpression(ctx: DictionaryExpressionContext): Expression {
    val dict: LinkedHashMap<Expression, Expression> = LinkedHashMap()
    for (dictEntry in ctx.dictionary().dictionaryEntry()) {
      dict[visit(dictEntry.expr(0))] = visit(dictEntry.expr(1))
    }
    return DictionaryExpression(dict)
  }

  override fun visitLiteralDictionaryExpression(ctx: LiteralDictionaryExpressionContext): Expression {
    val dict: LinkedHashMap<Expression, Expression> = LinkedHashMap()
    for (dictEntry in ctx.literalDictionary().literalDictionaryEntry()) {
      dict[SimpleExpression(VimString(dictEntry.literalDictionaryKey().text))] = visit(dictEntry.expr())
    }
    return DictionaryExpression(dict)
  }

  override fun visitIntExpression(ctx: IntExpressionContext): Expression {
    return SimpleExpression(VimInt(ctx.text))
  }

  override fun visitStringExpression(ctx: StringExpressionContext): Expression {
    var text = ctx.text
    val firstSymbol = text[0]
    if (firstSymbol == '"') {
      text = text
        .substring(1, text.length - 1)
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
    } else if (firstSymbol == '\'') {
      text = text
        .substring(1, text.length - 1)
        .replace("''", "'")
    }
    return SimpleExpression(VimString(text))
  }

  override fun visitListExpression(ctx: ListExpressionContext): Expression {
    return ListExpression((ctx.list().expr().map { visit(it) }.toMutableList()))
  }

  override fun visitBinExpression1(ctx: VimscriptParser.BinExpression1Context): Expression {
    val left = visit(ctx.expr(0))
    val right = visit(ctx.expr(1))
    val operatorString = ctx.binaryOperator1().text
    val operator = BinaryOperator.getByValue(operatorString) ?: throw RuntimeException()
    return BinExpression(left, right, operator)
  }

  override fun visitBinExpression2(ctx: VimscriptParser.BinExpression2Context): Expression {
    val left = visit(ctx.expr(0))
    val right = visit(ctx.expr(1))
    val operatorString = ctx.binaryOperator2().text

    return if (operatorString == "." && !containsSpaces(ctx) && evaluationResultCouldBeADictionary(left) && matchesLiteralDictionaryKey(ctx.expr(1).text)) {
      val index = SimpleExpression(VimString(ctx.expr(1).text))
      OneElementSublistExpression(index, left)
    } else if (operatorString == "-" && left is OneElementSublistExpression && !containsSpaces(ctx) && matchesLiteralDictionaryKey(
        ctx.expr(1).text
      )
    ) {
      val postfix = "-" + ctx.expr(1).text
      val newIndex = SimpleExpression(VimString((left.index as SimpleExpression).data.asString() + postfix))
      OneElementSublistExpression(newIndex, left.expression)
    } else if (operatorString == "." && !containsSpaces(ctx) && right is FunctionCallExpression && evaluationResultCouldBeADictionary(left)) {
      val index = SimpleExpression(VimString(right.functionName))
      FuncrefCallExpression(OneElementSublistExpression(index, left), right.arguments)
    } else {
      val operator = BinaryOperator.getByValue(operatorString) ?: throw RuntimeException()
      BinExpression(left, right, operator)
    }
  }

  private fun containsSpaces(ctx: ParserRuleContext): Boolean {
    for (child in ctx.children) {
      if (child.text.isBlank()) return true
    }
    return false
  }

  private fun matchesLiteralDictionaryKey(string: String): Boolean {
    return string.matches(Regex("[a-zA-Z0-9_-]+"))
  }

  private fun evaluationResultCouldBeADictionary(ctx: Expression): Boolean {
    return when (ctx) {
      is ListExpression, is UnaryExpression -> false
      is SimpleExpression -> ctx.data is VimDictionary
      else -> true
    }
  }

  override fun visitBinExpression3(ctx: VimscriptParser.BinExpression3Context): Expression {
    val left = visit(ctx.expr(0))
    val right = visit(ctx.expr(1))
    val operatorString = ctx.binaryOperator3().text
    val operator = BinaryOperator.getByValue(operatorString) ?: throw RuntimeException()
    return BinExpression(left, right, operator)
  }

  override fun visitBinExpression4(ctx: VimscriptParser.BinExpression4Context): Expression {
    val left = visit(ctx.expr(0))
    val right = visit(ctx.expr(1))
    val operatorString = ctx.binaryOperator4().text
    val operator = BinaryOperator.getByValue(operatorString) ?: throw RuntimeException()
    return BinExpression(left, right, operator)
  }

  override fun visitBinExpression5(ctx: VimscriptParser.BinExpression5Context): Expression {
    val left = visit(ctx.expr(0))
    val right = visit(ctx.expr(1))
    val operatorString = ctx.binaryOperator5().text
    val operator = BinaryOperator.getByValue(operatorString) ?: throw RuntimeException()
    return BinExpression(left, right, operator)
  }

  override fun visitUnaryExpression(ctx: UnaryExpressionContext): Expression {
    val expression = visit(ctx.expr())
    val operator = UnaryOperator.getByValue(ctx.getChild(0).text)
    return UnaryExpression(operator, expression)
  }

  override fun visitFloatExpression(ctx: FloatExpressionContext): Expression {
    return SimpleExpression(VimFloat(ctx.unsignedFloat().text.toDouble()))
  }

  override fun visitVariableExpression(ctx: VariableExpressionContext): Expression {
    return visitVariable(ctx.variable())
  }

  override fun visitWrappedExpression(ctx: WrappedExpressionContext): Expression? {
    return visit(ctx.expr())
  }

  override fun visitOptionExpression(ctx: OptionExpressionContext): Expression {
    return OptionExpression(ctx.option().optionName().text)
  }

  override fun visitTernaryExpression(ctx: TernaryExpressionContext): Expression {
    val condition = visit(ctx.expr(0))
    val then = visit(ctx.expr(1))
    val otherwise = visit(ctx.expr(2))
    return TernaryExpression(condition, then, otherwise)
  }

  override fun visitFunctionAsMethodCall1(ctx: VimscriptParser.FunctionAsMethodCall1Context): FunctionCallExpression {
    val functionCall = visitFunctionCall(ctx.functionCall())
    functionCall.arguments.add(0, visit(ctx.expr()))
    return functionCall
  }

  override fun visitFunctionAsMethodCall2(ctx: VimscriptParser.FunctionAsMethodCall2Context): LambdaFunctionCallExpression {
    val lambda = visitLambda(ctx.lambda())
    val arguments = mutableListOf(visit(ctx.expr()))
    arguments.addAll(ctx.functionArguments().functionArgument().mapNotNull { if (it.expr() != null) visit(it.expr()) else null })
    return LambdaFunctionCallExpression(lambda, arguments)
  }

  override fun visitFunctionCallExpression(ctx: FunctionCallExpressionContext): Expression {
    return visitFunctionCall(ctx.functionCall())
  }

  override fun visitFunctionCall(ctx: VimscriptParser.FunctionCallContext): FunctionCallExpression {
    val functionName = ctx.functionName().text
    var scope: Scope? = null
    if (ctx.functionScope() != null) {
      scope = Scope.getByValue(ctx.functionScope().text)
    }
    val functionArguments = ctx.functionArguments().functionArgument().mapNotNull { if (it.expr() != null) visit(it.expr()) else null }.toMutableList()
    return FunctionCallExpression(scope, functionName, functionArguments)
  }

  override fun visitLambdaFunctionCallExpression(ctx: VimscriptParser.LambdaFunctionCallExpressionContext): LambdaFunctionCallExpression {
    val lambda = visitLambda(ctx.lambda())
    val arguments = ctx.functionArguments().functionArgument().mapNotNull { if (it.expr() != null) visit(it.expr()) else null }
    return LambdaFunctionCallExpression(lambda, arguments)
  }

  override fun visitLambdaExpression(ctx: VimscriptParser.LambdaExpressionContext?): Expression {
    return super.visitLambdaExpression(ctx)
  }

  override fun visitLambda(ctx: VimscriptParser.LambdaContext): LambdaExpression {
    val arguments = ctx.argumentsDeclaration().variableName().map { it.text }
    val expr = visit(ctx.expr())
    return LambdaExpression(arguments, expr)
  }

  override fun visitSublistExpression(ctx: SublistExpressionContext): Expression {
    val ex = visit(ctx.expr(0))
    val from = if (ctx.from != null) visit(ctx.from) else null
    val to = if (ctx.to != null) visit(ctx.to) else null
    return SublistExpression(from, to, ex)
  }

  override fun visitOneElementSublistExpression(ctx: OneElementSublistExpressionContext): Expression {
    val ex = visit(ctx.expr(0))
    val fromTo = visit(ctx.expr(1))
    return OneElementSublistExpression(fromTo, ex)
  }

  override fun visitEnvVariableExpression(ctx: EnvVariableExpressionContext): Expression {
    return EnvVariableExpression(ctx.envVariable().envVariableName().text)
  }

  override fun visitRegisterExpression(ctx: RegisterExpressionContext): Expression {
    return Register(ctx.text.replaceFirst("@", "")[0])
  }

  override fun visitVariable(ctx: VariableContext): Expression {
    val scope = if (ctx.variableScope() == null) null else Scope.getByValue(ctx.variableScope().text)
    return Variable(scope, ctx.variableName().text)
  }

  override fun visitFalsyExpression(ctx: FalsyExpressionContext): Expression {
    val left = visit(ctx.expr(0))
    val right = visit(ctx.expr(1))
    return FalsyExpression(left, right)
  }

  override fun visitBlobExpression(ctx: BlobExpressionContext?): Expression {
    TODO()
  }
}