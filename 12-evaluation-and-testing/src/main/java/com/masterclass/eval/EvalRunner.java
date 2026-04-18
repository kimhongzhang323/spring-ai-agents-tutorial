package com.masterclass.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Loads a YAML golden dataset, calls the agent under test for each case,
 * runs the LLM judge, and returns an EvalReport.
 *
 * The agent under test is represented by an AgentUnderTest functional interface
 * so any ChatClient-backed service can be wired in without coupling this runner
 * to a specific module.
 */
@Service
public class EvalRunner {

    private static final Logger log = LoggerFactory.getLogger(EvalRunner.class);
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
    private final LlmJudge judge;
    private final MeterRegistry meters;

    public EvalRunner(LlmJudge judge, MeterRegistry meters) {
        this.judge  = judge;
        this.meters = meters;
    }

    public EvalReport run(Resource datasetResource, AgentUnderTest agent, double passThreshold)
            throws IOException {

        List<EvalCase> cases = yaml.readValue(
                datasetResource.getInputStream(),
                new TypeReference<>() {});

        log.info("Running eval on {} cases from {}", cases.size(), datasetResource.getFilename());

        List<EvalScore> scores = cases.stream().map(c -> {
            String actual = agent.answer(c.question());
            EvalScore score = judge.evaluate(c, actual);
            meters.counter("eval.case",
                    "passed", String.valueOf(score.passed(passThreshold))).increment();
            log.debug("Case {}: faithfulness={}, relevance={}, passed={}",
                    c.id(), score.faithfulness(), score.relevance(), score.passed(passThreshold));
            return score;
        }).toList();

        return new EvalReport(scores, passThreshold);
    }

    @FunctionalInterface
    public interface AgentUnderTest {
        String answer(String question);
    }
}
