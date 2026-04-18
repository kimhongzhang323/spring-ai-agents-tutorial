package com.masterclass.tools.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Safe arithmetic tool — demonstrates that tools should be narrow, predictable, and side-effect-free
 * where possible. No external calls = no circuit breaker needed.
 *
 * Anti-pattern: never expose eval() or any dynamic expression evaluator as a tool.
 * The LLM can craft inputs that execute arbitrary code via script engines.
 */
@Component
public class CalculatorTool {

    @Tool(description = """
            Perform a basic arithmetic calculation: add, subtract, multiply, or divide two numbers.
            Use this tool whenever the user asks you to calculate, compute, or work out a math expression
            involving two numbers and one operation.
            Supported operations: ADD, SUBTRACT, MULTIPLY, DIVIDE.
            Returns the numeric result. If dividing by zero, returns an error message.
            """)
    public CalculationResult calculate(double a, String operation, double b) {
        return switch (operation.toUpperCase()) {
            case "ADD"      -> new CalculationResult(a + b, null);
            case "SUBTRACT" -> new CalculationResult(a - b, null);
            case "MULTIPLY" -> new CalculationResult(a * b, null);
            case "DIVIDE"   -> b == 0
                    ? new CalculationResult(null, "Division by zero is undefined")
                    : new CalculationResult(a / b, null);
            default -> new CalculationResult(null, "Unknown operation: " + operation +
                    ". Use ADD, SUBTRACT, MULTIPLY, or DIVIDE.");
        };
    }

    public record CalculationResult(Double result, String error) {}
}
