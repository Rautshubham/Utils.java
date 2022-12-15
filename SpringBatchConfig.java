package com.en.config;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;

import com.en.batch.model.BatchRequestBean;
import com.en.batch.processor.CardRefundProcessor;
import com.en.batch.processor.ManualAdjustmentProcessor;
import com.en.batch.processor.Processor;
import com.en.cardRefund.model.CardRefundBean;
import com.en.manualAdjustment.model.ManualAdjustmentBean;
import com.en.rate.model.RateUploadRequestBean;

@Configuration
@EnableBatchProcessing
public class SpringBatchConfig {
	private static Logger log = LogManager.getLogger(SpringBatchConfig.class.getPackage().getName());
	@Autowired
	Environment env;
	
	@Autowired
	DataSource dataSource;

	@Autowired
	JobBuilderFactory jobBuilderFactory;

	@Autowired
	StepBuilderFactory stepBuilderFactory;

	/*@Bean
	public DataSourceTransactionManager dataSourceTransactionManager(@Qualifier("dataSource1") DataSource dataSource){
		DataSourceTransactionManager  manager = new DataSourceTransactionManager(dataSource);
		return manager;
	}*/


	@Bean 
	public ResourcelessTransactionManager transactionManager(){
		return new ResourcelessTransactionManager();
	}


	@Bean
	public MapJobRepositoryFactoryBean mapJobRepositoryFactory(ResourcelessTransactionManager transactionManager) throws Exception{
		MapJobRepositoryFactoryBean factory = new MapJobRepositoryFactoryBean(transactionManager);

		factory.afterPropertiesSet();
		return factory;
	}


	/*@Bean
	public JobRepository jobRepositoryFactoryBean() throws Exception{
		log.doLog(2, className, "jobRepositoryFactoryBean", "inside jobrepository method");
		JobRepositoryFactoryBean jb = new JobRepositoryFactoryBean();
		jb.setDataSource(dataSource);
		jb.setDatabaseType("DB2");
		jb.setTransactionManager(new DataSourceTransactionManager(dataSource));
		jb.afterPropertiesSet();
		return jb.getObject();
	}*/

	@Bean
	public JobRepository jobRepository(MapJobRepositoryFactoryBean mapJobRepositoryFactory) throws Exception{
		return mapJobRepositoryFactory.getObject();
	}

	@Bean
	public SimpleJobLauncher jobLauncher(JobRepository jobRepository){
		SimpleJobLauncher simpleJobLauncher = new SimpleJobLauncher();
		simpleJobLauncher.setJobRepository(jobRepository);
		return simpleJobLauncher;
	}

	@Bean
	public Job job(JobBuilderFactory jobBuilderFactory,
			StepBuilderFactory stepBuilderFactory,
			ItemReader<BatchRequestBean> itemReader,
			ItemProcessor<BatchRequestBean, BatchRequestBean> itemProcessor,
			ItemWriter<BatchRequestBean> itemWriter
			) {

		Step step = stepBuilderFactory.get("ETL-file-load")
				.<BatchRequestBean, BatchRequestBean>chunk(Integer.parseInt(env.getProperty("reload_chunk_size")))
				.reader(itemReader)
				.processor(itemProcessor)
				.writer(itemWriter)
				.build();


		return jobBuilderFactory.get("ETL-Load")
				.incrementer(new RunIdIncrementer())
				.start(step)
				.build();
	}

	/*    @Bean
    @StepScope
    public FlatFileItemReader<BatchRequestBean> itemReader(@Value("#{jobParameters['fileName']}")String fileName) {

        FlatFileItemReader<BatchRequestBean> flatFileItemReader = new FlatFileItemReader<>();
        flatFileItemReader.setResource(new FileSystemResource(fileName));
        flatFileItemReader.setName("CSV-Reader");
        flatFileItemReader.setLinesToSkip(1);
        flatFileItemReader.setLineMapper(lineMapper());
        return flatFileItemReader;
    }*/

	@Bean
    @StepScope
    public FlatFileItemReader<BatchRequestBean> itemReader(@Value("#{jobParameters['fileName']}")String fileName) {
    	    return new FlatFileItemReaderBuilder<BatchRequestBean>()
    	        .name("itemReader")
    	        .resource(new FileSystemResource(fileName))
    	        .linesToSkip(1)
    	        .delimited()
    	        .delimiter("|").names(new String[]{"cardNumber", "currencyCode", "amount", "activityType", "paymentMode", "travelPurpose", "bordeauxType", "bordeauxNumber"})
    	        .fieldSetMapper(new BeanWrapperFieldSetMapper<BatchRequestBean>() {{
    	            setTargetType(BatchRequestBean.class);
    	         }})
    	        .build();
    }

	/*@Bean
    public LineMapper<BatchRequestBean> lineMapper() {

        DefaultLineMapper<BatchRequestBean> defaultLineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();

        lineTokenizer.setDelimiter("|");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames(new String[]{"CardNumber", "CurrencyCode", "Amount", "ActivityType", "PaymentMode", "TravelPurpose", "BordeauxType", "BordeauxNumber"});

        BeanWrapperFieldSetMapper<BatchRequestBean> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(BatchRequestBean.class);

        defaultLineMapper.setLineTokenizer(lineTokenizer);
        defaultLineMapper.setFieldSetMapper(fieldSetMapper);

        return defaultLineMapper;
    }*/


	@Bean 
	@StepScope
	public ItemProcessor<BatchRequestBean,BatchRequestBean> processor(@Value("#{jobParameters['fileName']}")String fileName,
			@Value("#{jobParameters['userId']}")String userId,
			@Value("#{jobParameters['remarks']}")String remarks,
			@Value("#{jobParameters['branchId']}")String branchId,
			@Value("#{jobParameters['actType']}")String actType){
		return new Processor(fileName,userId,remarks,branchId,actType);
	}
	
//	~Ameya
	//Rate Upload chnages starts here ~Ameya
	@Bean(name="RateUploadJob")
	public Job rateUploadJob(JobBuilderFactory jobBuilderFactory,
			StepBuilderFactory stepBuilderFactory,
			ItemReader<RateUploadRequestBean> itemReaderRate,
			ItemProcessor<RateUploadRequestBean, RateUploadRequestBean> itemProcessor,
			ItemWriter<RateUploadRequestBean> itemWriter
			) {

		Step step = stepBuilderFactory.get("ETL-file-RateUpload")
				.<RateUploadRequestBean, RateUploadRequestBean>chunk(Integer.parseInt(env.getProperty("reload_chunk_size")))
				.reader(itemReaderRate)
				.processor(itemProcessor)
				.writer(itemWriter)
				.build();


		return jobBuilderFactory.get("ETL-Load")
				.incrementer(new RunIdIncrementer())
				.start(step)
				.build();
	}
	
//	temp needs to work on
	@Bean 
	@StepScope
	public ItemProcessor<RateUploadRequestBean,RateUploadRequestBean> rateUploadProcessor(@Value("#{jobParameters['fileName']}")String fileName,
			@Value("#{jobParameters['userId']}")String userId,
			@Value("#{jobParameters['remarks']}")String remarks,
			@Value("#{jobParameters['branchId']}")String branchId,
			@Value("#{jobParameters['actType']}")String actType){
//		return new Processor(fileName,userId,remarks,branchId,actType);
		
		return p->{return p;};
	}
	
	@Bean
    @StepScope
    public FlatFileItemReader<RateUploadRequestBean> itemReaderRate(@Value("#{jobParameters['fileName']}")String fileName) {
    	    return new FlatFileItemReaderBuilder<RateUploadRequestBean>()
    	        .name("itemReader")
    	        .resource(new FileSystemResource(fileName))
    	        .linesToSkip(1)
    	        .delimited()
    	        .delimiter("|").names(new String[]{"name", "desc"})
    	        .fieldSetMapper(new BeanWrapperFieldSetMapper<RateUploadRequestBean>() {{
    	            setTargetType(RateUploadRequestBean.class);
    	         }})
    	        .build();
    }
	
	//Rate Upload changes ends here ~Ameya
	
	//Card Refund Upload changes starts here ~Ameya
	
	@Bean(name="CardRefundBatchJob")
	public Job cardRefundBatchJob(JobBuilderFactory jobBuilderFactory,
			StepBuilderFactory stepBuilderFactory,
			ItemReader<CardRefundBean> cardRefundReader,
			ItemProcessor<CardRefundBean, CardRefundBean> cardRefundProcessor,
			ItemWriter<CardRefundBean> itemWriter
			) {

		Step step = stepBuilderFactory.get("ETL-file-CardRefundUpload")
				.<CardRefundBean, CardRefundBean>chunk(Integer.parseInt(env.getProperty("reload_chunk_size")))
				.reader(cardRefundReader)
				.processor(cardRefundProcessor)
				.writer(itemWriter)
				.build();


		return jobBuilderFactory.get("ETL-CardRefundUpload")
				.incrementer(new RunIdIncrementer())
				.start(step)
				.build();
	}
	

	@Bean 
	@StepScope
	public ItemProcessor<CardRefundBean,CardRefundBean> cardRefundProcessor(@Value("#{jobParameters['fileName']}")String fileName,
			@Value("#{jobParameters['userId']}")String userId,
			@Value("#{jobParameters['branchId']}")String branchId){
		return new CardRefundProcessor(fileName,userId,branchId);
		
		
	}
	
	@Bean
    @StepScope
    public FlatFileItemReader<CardRefundBean> cardRefundReader(@Value("#{jobParameters['fileName']}")String fileName) {
    	    return new FlatFileItemReaderBuilder<CardRefundBean>()
    	        .name("cardRefundReader")
    	        .resource(new FileSystemResource(fileName))
    	        .linesToSkip(1)
    	        .delimited()
    	        .delimiter("|").names(new String[]{"cardNumber", "paymentMode","travelPurpose","currency",
    	        		"refundType","amount","currencyRate"})
    	        .fieldSetMapper(new BeanWrapperFieldSetMapper<CardRefundBean>() {{
    	            setTargetType(CardRefundBean.class);
    	         }})
    	        .build();
    }
	
	//Manual Adjustment Upload changes starts here ~Ameya
	
		@Bean(name="ManualAdjustmentBatchJob")
		public Job manualAdjustmentJob(JobBuilderFactory jobBuilderFactory,
				StepBuilderFactory stepBuilderFactory,
				ItemReader<ManualAdjustmentBean> manualAdjustmentReader,
				ItemProcessor<ManualAdjustmentBean, ManualAdjustmentBean> itemProcessor,
				ItemWriter<ManualAdjustmentBean> itemWriter
				) {

			Step step = stepBuilderFactory.get("ETL-file-ManualAdjustmentUpload")
					.<ManualAdjustmentBean, ManualAdjustmentBean>chunk(Integer.parseInt(env.getProperty("reload_chunk_size")))
					.reader(manualAdjustmentReader)
					.processor(itemProcessor)
					.writer(itemWriter)
					.build();


			return jobBuilderFactory.get("ETL-ManualAdjustmentUpload")
					.incrementer(new RunIdIncrementer())
					.start(step)
					.build();
		}
		
//		temp needs to work on
		@Bean 
		@StepScope
		public ItemProcessor<ManualAdjustmentBean,ManualAdjustmentBean> manualAdjustmentProcessor(
				@Value("#{jobParameters['fileName']}")String fileName,
				@Value("#{jobParameters['userId']}")String userId,
				@Value("#{jobParameters['branchId']}")String branchId){
			return new ManualAdjustmentProcessor(fileName,userId,branchId);
			
			
		}
		
		@Bean
	    @StepScope
	    public FlatFileItemReader<ManualAdjustmentBean> manualAdjustmentReader(@Value("#{jobParameters['fileName']}")String fileName) {
	    	    return new FlatFileItemReaderBuilder<ManualAdjustmentBean>()
	    	        .name("manualAdjustmentReader")
	    	        .resource(new FileSystemResource(fileName))
	    	        .linesToSkip(1)
	    	        .delimited()
	    	        .delimiter("|").names(new String[]{"cardNumber", "adjustmentType","adjustmentReason","currency","amount"})
	    	        .fieldSetMapper(new BeanWrapperFieldSetMapper<ManualAdjustmentBean>() {{
	    	            setTargetType(ManualAdjustmentBean.class);
	    	         }})
	    	        .build();
	    }

}
