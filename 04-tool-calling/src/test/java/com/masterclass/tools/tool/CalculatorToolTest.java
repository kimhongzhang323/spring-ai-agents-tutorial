package com.masterclass.tools.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CalculatorToolTest {

    private final CalculatorTool calculator = new CalculatorTool();

    @ParameterizedTest(name = "{0} {1} {2} = {3}")
    @CsvSource({
            "10.0, ADD,      5.0,  15.0",
            "10.0, SUBTRACT, 3.0,   7.0",
            "4.0,  MULTIPLY, 5.0,  20.0",
            "20.0, DIVIDE,   4.0,   5.0",
    })
    void performsBasicArithmetic(double a, String op, double b, double expected) {
        var result = calculator.calculate(a, op, b);
        assertThat(result.error()).isNull();
        assertThat(result.result()).isEqualTo(expected);
    }

    @Test
    void divisionByZeroReturnsError() {
        var result = calculator.calculate(10, "DIVIDE", 0);
        assertThat(result.result()).isNull();
        assertThat(result.error()).containsIgnoringCase("zero");
    }

    @Test
    void unknownOperationReturnsError() {
        var result = calculator.calculate(1, "POWER", 2);
        assertThat(result.error()).containsIgnoringCase("Unknown operation");
    }

    @Test
    void operationIsCaseInsensitive() {
        assertThat(calculator.calculate(2, "add", 3).result()).isEqualTo(5.0);
        assertThat(calculator.calculate(2, "Add", 3).result()).isEqualTo(5.0);
    }
}
