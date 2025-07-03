// src/main/java/tmoney/tnz/bmsbatch/common/CommonMultiResourceItemReaderFactory.java
package tmoney.gbi.bmsbatch.common.reader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tmoney.gbi.bmsbatch.properties.FileJobProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CommonMultiResourceItemReaderFactory {

    public <T> MultiResourceItemReader<T> create(FileJobProperties properties, Class<T> targetType, StepExecution stepExecution) {
        log.info("Creating MultiResourceItemReader for path: {}", properties.getInputPath());

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try {
            resources = resolver.getResources("file:" + properties.getInputPath() + properties.getFilePattern());
            if (resources.length == 0) {
                log.warn("No files found for pattern: {}{}", properties.getInputPath(), properties.getFilePattern());
            } else {
                log.info("Found {} files to process.", resources.length);
            }
        } catch (IOException e) {
            log.error("Failed to find file resources.", e);
            throw new RuntimeException("Failed to find file resources.", e);
        }

        List<String> resourcePaths = Arrays.stream(resources)
                .map(resource -> {
                    try {
                        return resource.getFile().getAbsolutePath();
                    } catch (IOException e) {
                        log.error("Could not get file path from resource: {}", resource.getFilename(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        stepExecution.getExecutionContext().put("processedResources", resourcePaths);

        FlatFileItemReader<T> delegate = new FlatFileItemReaderBuilder<T>()
                .name(targetType.getSimpleName() + "FileReader")
                .linesToSkip(0)
                .delimited()
                .delimiter(properties.getDelimiter())
                .names(properties.getNames())
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(targetType);
                }})
                .build();

        return new MultiResourceItemReaderBuilder<T>()
                .name("multi" + targetType.getSimpleName() + "FileReader")
                .resources(resources)
                .delegate(delegate)
                .build();
    }
}