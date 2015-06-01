package de.tudarmstadt.stg.monto.ecmascript.service;

import de.tudarmstadt.stg.monto.ecmascript.antlr.ECMAScriptLexer;
import de.tudarmstadt.stg.monto.ecmascript.ast.Category;
import de.tudarmstadt.stg.monto.ecmascript.ast.Token;
import de.tudarmstadt.stg.monto.ecmascript.ast.Tokens;
import de.tudarmstadt.stg.monto.ecmascript.message.Message;
import de.tudarmstadt.stg.monto.ecmascript.message.ProductMessage;
import de.tudarmstadt.stg.monto.ecmascript.message.VersionMessage;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.zeromq.ZMQ;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ECMAScriptTokenizer extends ECMAScriptService {

    private ECMAScriptLexer lexer = new ECMAScriptLexer(new ANTLRInputStream());

    public ECMAScriptTokenizer(String address, ZMQ.Context context) {
        super(address, context);
    }

    @Override
    public ProductMessage processMessage(List<Message> messages) {
        VersionMessage version = (VersionMessage) messages.stream().filter(msg -> msg instanceof VersionMessage).findFirst().get();
        lexer.setInputStream(new ANTLRInputStream(version.getContents()));
        List<Token> tokens = lexer.getAllTokens().stream().map(token -> convertToken(token)).collect(Collectors.toList());

        return new ProductMessage(
                version.getSource(),
                version.getVersionId(),
                "json",
                "",
                Tokens.encode(tokens).toJSONString(),
                1,
                "tokens",
                ""
        );
    }

    private Token convertToken(org.antlr.v4.runtime.Token token) {
        int offset = token.getStartIndex();
        int length = token.getStopIndex() - offset + 1;

        Category category;
        switch (token.getType()) {

            case ECMAScriptLexer.SingleLineComment:
            case ECMAScriptLexer.MultiLineComment:
                category = Category.COMMENT;
                break;

            case ECMAScriptLexer.Const:
            case ECMAScriptLexer.NullLiteral:
                category = Category.CONSTANT;
                break;

            case ECMAScriptLexer.StringLiteral:
            case ECMAScriptLexer.RegularExpressionLiteral:
                category = Category.STRING;
                break;

            case ECMAScriptLexer.DecimalLiteral:
            case ECMAScriptLexer.HexIntegerLiteral:
            case ECMAScriptLexer.OctalIntegerLiteral:
                category = Category.NUMBER;
                break;

            case ECMAScriptLexer.BooleanLiteral:
                category = Category.BOOLEAN;
                break;

            case ECMAScriptLexer.Identifier:
                category = Category.IDENTIFIER;
                break;

            case ECMAScriptLexer.If:
            case ECMAScriptLexer.Else:
            case ECMAScriptLexer.Switch:
                category = Category.CONDITIONAL;
                break;

            case ECMAScriptLexer.For:
            case ECMAScriptLexer.Do:
            case ECMAScriptLexer.While:
            case ECMAScriptLexer.Continue:
            case ECMAScriptLexer.Break:
                category = Category.REPEAT;
                break;

            case ECMAScriptLexer.Case:
            case ECMAScriptLexer.Default:
                category = Category.LABEL;
                break;

            case ECMAScriptLexer.Plus:
            case ECMAScriptLexer.PlusAssign:
            case ECMAScriptLexer.Minus:
            case ECMAScriptLexer.MinusAssign:
            case ECMAScriptLexer.Multiply:
            case ECMAScriptLexer.MultiplyAssign:
            case ECMAScriptLexer.Divide:
            case ECMAScriptLexer.DivideAssign:
            case ECMAScriptLexer.Modulus:
            case ECMAScriptLexer.ModulusAssign:
            case ECMAScriptLexer.PlusPlus:
            case ECMAScriptLexer.MinusMinus:
            case ECMAScriptLexer.And:
            case ECMAScriptLexer.BitAndAssign:
            case ECMAScriptLexer.BitAnd:
            case ECMAScriptLexer.Or:
            case ECMAScriptLexer.BitOrAssign:
            case ECMAScriptLexer.BitOr:
            case ECMAScriptLexer.BitXOr:
            case ECMAScriptLexer.BitXorAssign:
            case ECMAScriptLexer.GreaterThanEquals:
            case ECMAScriptLexer.MoreThan:
            case ECMAScriptLexer.LessThan:
            case ECMAScriptLexer.LessThanEquals:
            case ECMAScriptLexer.LeftShiftArithmeticAssign:
            case ECMAScriptLexer.RightShiftArithmeticAssign:
            case ECMAScriptLexer.Assign:
            case ECMAScriptLexer.Equals:
            case ECMAScriptLexer.IdentityEquals:
            case ECMAScriptLexer.NotEquals:
            case ECMAScriptLexer.IdentityNotEquals:
            case ECMAScriptLexer.QuestionMark:
            case ECMAScriptLexer.Instanceof:
                category = Category.OPERATOR;
                break;

            case ECMAScriptLexer.Try:
            case ECMAScriptLexer.Catch:
            case ECMAScriptLexer.Throw:
            case ECMAScriptLexer.Finally:
                category = Category.EXCEPTION;
                break;

            case ECMAScriptLexer.Class:
            case ECMAScriptLexer.Enum:
            case ECMAScriptLexer.Interface:
                category = Category.STRUCTURE;
                break;

            case ECMAScriptLexer.Extends:
            case ECMAScriptLexer.Implements:
            case ECMAScriptLexer.Import:
            case ECMAScriptLexer.Package:
            case ECMAScriptLexer.This:
            case ECMAScriptLexer.Super:
            case ECMAScriptLexer.New:
            case ECMAScriptLexer.Return:
            case ECMAScriptLexer.Var:
            case ECMAScriptLexer.Function:
                category = Category.KEYWORD;
                break;

            case ECMAScriptLexer.OpenParen:
            case ECMAScriptLexer.CloseParen:
            case ECMAScriptLexer.OpenBrace:
            case ECMAScriptLexer.CloseBrace:
            case ECMAScriptLexer.OpenBracket:
            case ECMAScriptLexer.CloseBracket:
                category = Category.PARENTHESIS;
                break;

            case ECMAScriptLexer.Comma:
            case ECMAScriptLexer.Colon:
            case ECMAScriptLexer.Dot:
            case ECMAScriptLexer.SemiColon:
                category = Category.DELIMITER;
                break;

            case ECMAScriptLexer.WhiteSpaces:
            case ECMAScriptLexer.LineTerminator:
                category = Category.WHITESPACE;
                break;

            default:
                category = Category.UNKNOWN;
        }

        return new Token(offset, length, category);
    }

}
