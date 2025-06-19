package tmoney.tnz.bmsbatch.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import tmoney.tnz.bmsbatch.domain.Person;
import tmoney.tnz.bmsbatch.mapper.PersonMapper;

import java.io.File;
import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BmsTxtFileJob {

    private final PersonMapper personMapper;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final String folderPath = "/Users/smkim/intellij/bms_batch";

    @Bean
    public Job personJob() {
        return new JobBuilder("personJob", jobRepository)
                .start(personStep())
                .build();
    }

    @Bean
    public Step personStep() {
        return new StepBuilder("personStep", jobRepository)
                .<Person, Person>chunk(10, transactionManager)
                .reader(multiFileReader())
                .writer(compositeWriter())
                .build();
    }

    @Bean
    public MultiResourceItemReader<Person> multiFileReader() {
        File dir = new File(folderPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));

        MultiResourceItemReader<Person> reader = new MultiResourceItemReader<>();
        if (files != null) {
            Resource[] resources = Arrays.stream(files)
                    .map(FileSystemResource::new)
                    .toArray(Resource[]::new);
            reader.setResources(resources);
        }
        reader.setDelegate(flatFileReader());
        return reader;
    }

    @Bean
    public FlatFileItemReader<Person> flatFileReader() {
        FlatFileItemReader<Person> reader = new FlatFileItemReader<>();
        reader.setLinesToSkip(0);
        reader.setLineMapper(new DefaultLineMapper<>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setDelimiter(",");
                setNames("id", "name", "date");
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                setTargetType(Person.class);
            }});
        }});
        return reader;
    }

    @Bean
    public ItemWriter<Person> compositeWriter() {
        return new ItemWriter<>() {
            @Override
            public void write(Chunk<? extends Person> items) throws Exception {
                for (Person person : items) {
                    personMapper.insert(person);
                }
                deleteProcessedFiles();
            }
        };
    }

    private void deleteProcessedFiles() {
        File dir = new File(folderPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }
}
