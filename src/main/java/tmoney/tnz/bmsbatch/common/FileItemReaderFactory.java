package tmoney.tnz.bmsbatch.common;

import lombok.RequiredArgsConstructor;
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
import tmoney.tnz.bmsbatch.domain.Person;
import tmoney.tnz.bmsbatch.properties.FileJobProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileItemReaderFactory {

    /**
     * 설정값을 기반으로 다중 파일 리더(MultiResourceItemReader)를 생성합니다.
     * @param properties 파일 처리 설정
     * @param stepExecution 현재 스텝의 Execution Context
     * @return 구성된 MultiResourceItemReader
     */
    public MultiResourceItemReader<Person> createMultiResourceReader(FileJobProperties properties, StepExecution stepExecution) {
        log.info("Reading files from path: {} with pattern: {}", properties.getInputPath(), properties.getFilePattern());

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try {
            resources = resolver.getResources("file:" + properties.getInputPath() + properties.getFilePattern());
        } catch (IOException e) {
            log.error("Failed to find file resources.", e);
            throw new RuntimeException("Failed to find file resources.", e);
        }

        // 후처리 리스너에서 파일을 삭제할 수 있도록 파일 경로를 ExecutionContext에 저장합니다.
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

        return new MultiResourceItemReaderBuilder<Person>()
                .name("multiFileItemReader")
                .resources(resources)
                .delegate(createFlatFileReader(properties)) // FlatFile 리더를 위임받아 사용
                .build();
    }

    /**
     * 설정값을 기반으로 단일 파일 리더(FlatFileItemReader)를 생성합니다.
     * @param properties 파일 처리 설정
     * @return 구성된 FlatFileItemReader
     */
    private FlatFileItemReader<Person> createFlatFileReader(FileJobProperties properties) {
        return new FlatFileItemReaderBuilder<Person>()
                .name("flatFileItemReader")
                .linesToSkip(0)
                .delimited()
                .delimiter(properties.getDelimiter()) // 설정값으로 구분자 지정
                .names(properties.getNames())         // 설정값으로 필드명 지정
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(Person.class);
                }})
                .build();
    }
}
