package com.masterclass.tools;

import com.masterclass.tools.tool.CalculatorTool;
import com.masterclass.tools.tool.CurrencyTool;
import com.masterclass.tools.tool.StockPriceTool;
import com.masterclass.tools.tool.WeatherTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that every tool method has a non-empty @Tool description.
 * An empty description means the LLM has no guidance on when to call the tool —
 * it will either never call it, or call it randomly.
 *
 * This test runs at build time without an LLM and catches "I forgot to write a description" mistakes.
 */
class ToolAnnotationTest {

    @Test
    void weatherToolHasDescriptivAnnotation() {
        assertToolDescriptionIsSubstantial(WeatherTool.class, "getCurrentWeather");
    }

    @Test
    void calculatorToolHasDescriptivAnnotation() {
        assertToolDescriptionIsSubstantial(CalculatorTool.class, "calculate");
    }

    @Test
    void stockToolHasDescriptivAnnotation() {
        assertToolDescriptionIsSubstantial(StockPriceTool.class, "getStockPrice");
    }

    @Test
    void currencyToolHasDescriptivAnnotation() {
        assertToolDescriptionIsSubstantial(CurrencyTool.class, "convert");
    }

    private void assertToolDescriptionIsSubstantial(Class<?> toolClass, String methodName) {
        Method method = Arrays.stream(toolClass.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Method " + methodName + " not found on " + toolClass));

        Tool annotation = method.getAnnotation(Tool.class);
        assertThat(annotation)
                .as("@Tool annotation missing on %s.%s", toolClass.getSimpleName(), methodName)
                .isNotNull();
        assertThat(annotation.description())
                .as("@Tool description on %s.%s must be at least 50 chars — write it for the LLM, not for developers",
                        toolClass.getSimpleName(), methodName)
                .isNotBlank()
                .hasSizeGreaterThan(50);
    }
}
