package de.uni_marburg.schematch.matching;

import de.uni_marburg.schematch.preprocessing.tokenization.Tokenizer;
import de.uni_marburg.schematch.preprocessing.tokenization.TokenizerFactory;
import de.uni_marburg.schematch.utils.Configuration;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class MatcherFactory {
    private static final Logger log = LogManager.getLogger(MatcherFactory.class);

    private Map<String, List<Tokenizer>> tokenizers;
    private int numTokenizers;

    /**
     * Instantiates all tokenizers as specified in first_line_tokenizers.yaml
     * @throws Exception when reflection goes wrong
     */
    private void createTokenizers() throws Exception {
        this.tokenizers = new HashMap<>();
        TokenizerFactory tokenizerFactory = new TokenizerFactory();

        Configuration config = Configuration.getInstance();

        for (String tokenizerName : config.getTokenizerConfigurations().keySet()) {
            List<Tokenizer> tokenizerInstances = tokenizerFactory.createTokenizerInstances(tokenizerName);
            this.numTokenizers += tokenizerInstances.size();
            tokenizers.put(tokenizerName, tokenizerInstances);
        }

        if (this.numTokenizers == 0) {
            String e = "Using a tokenized matcher but there are no tokenizers configured.";
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Instantiates all configurations for a given matcher as specified in first_line_matchers.yaml
     * @return List of all configured matcher instances
     * @throws Exception when reflection goes wrong
     */
    public List<Matcher> createMatcherInstances(String matcherName) throws Exception {
        Configuration config = Configuration.getInstance();

        List<Configuration.MatcherConfiguration> matcherConfigurations = config.getMatcherConfigurations().get(matcherName);
        int numConfigs = matcherConfigurations.size();

        String name = matcherConfigurations.get(0).getName();
        String packageName = matcherConfigurations.get(0).getPackageName();
        Class<?> matcherClass = Class.forName(Configuration.MATCHING_PACKAGE_NAME + "." + packageName + "." + name);

        List<Matcher> matcherInstances = new ArrayList<>();
        // TODO: find better way to figure out if current matcher is tokenized
        Matcher m = (Matcher) matcherClass.getConstructor().newInstance();
        if (m instanceof TokenizedMatcher) {
            // first tokenized matcher, create tokenizers first
            if (this.tokenizers == null) {
                createTokenizers();
            }
            // for every tokenizer, initialize matcher for all matcher configurations
            log.info("Instantiating tokenized matcher " + matcherName + " with " + numConfigs + " different configurations and " +
                    this.numTokenizers + " different tokenizers (" + numConfigs*this.numTokenizers + " total)");
            for (String tokenizerName : this.tokenizers.keySet()) {
                for (Tokenizer tokenizer : this.tokenizers.get(tokenizerName)) {
                    for (Configuration.MatcherConfiguration matcherConfiguration : matcherConfigurations) {
                        TokenizedMatcher matcher = (TokenizedMatcher) matcherClass.getConstructor().newInstance();
                        matcher.configure(matcherConfiguration);
                        matcher.setTokenizer(tokenizer);
                        matcherInstances.add(matcher);
                    }
                }
            }
        } else {
            // initialize matcher for all matcher configurations
            log.info("Instantiating matcher " + matcherName + " with " + numConfigs + " different configurations");
            for (Configuration.MatcherConfiguration matcherConfiguration : matcherConfigurations) {
                Matcher matcher = (Matcher) matcherClass.getConstructor().newInstance();
                matcher.configure(matcherConfiguration);
                matcherInstances.add(matcher);
            }
        }

        return matcherInstances;
    }

    /**
     * Instantiates all matchers as specified in first_line_matchers.yaml
     * @return Map of matcher names to list of all configured matcher instances
     * @throws Exception when reflection goes wrong
     */
    public Map<String, List<Matcher>> createMatchersFromConfig() throws Exception {
        Map<String, List<Matcher>> matchers = new HashMap<>();
        Configuration config = Configuration.getInstance();

        for (String matcherName : config.getMatcherConfigurations().keySet()) {
            matchers.put(matcherName, createMatcherInstances(matcherName));
        }

        return matchers;
    }
}
