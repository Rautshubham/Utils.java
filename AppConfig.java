/**
 * @author  Deepak Kumar
 * @version 1.0
 * @purpose This class is used to define application configuration and create class beans.
 *
 * @History
 * ===============================================================================================================================================
 *     Version         Date             Author                 Purpose
 * ===============================================================================================================================================
 *     1.0                      27-11-20        Deepak Kumar                 This class is used to define application configuration and create class beans.
 * ===============================================================================================================================================
 *
 */
package com.en.config;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.NoSuchPaddingException;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jndi.JndiTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;

import com.en.common.SessionDTO;
import com.en.common.interceptor.RequestHandlerInterceptor;
import com.en.common.util.AES;
import com.en.config.core.SpringMvcInitializer;
import com.en.institution.services.InstitutionService;
import com.en.login.authentication.CustomAuthenticationProvider;
import com.en.login.authentication.CustomAuthorizeRequests;
import com.en.login.services.CustomUserService;
import com.ibm.as400.access.AS400JDBCManagedConnectionPoolDataSource;

@EnableAsync
@EnableWebMvc
@Configuration
@EnableTransactionManagement
@EnableScheduling
@ComponentScan({ "com.en.*" })
@Import({ SecurityConfig.class })
@PropertySource("classpath:config.properties")
@PropertySource("classpath:EmailConfig.properties")
//@PropertySource("file:${user.home}/EmailConfig.properties")
@PropertySource("classpath:SystemConfig_en.properties")
@PropertySource("classpath:MasterConfig_en.properties")
@PropertySource("classpath:CardSupportConfig_en.properties")
@PropertySource("classpath:CardManagementConfig_en.properties")
@PropertySource("classpath:BulkProcessConfig_en.properties")
public class AppConfig extends WebMvcConfigurerAdapter {
    @Autowired
    Environment   env;
    @Autowired
    SessionDTO sessionDTO;
    
    @Autowired
    CustomUserService userService;
    
    @Autowired
    InstitutionService institutionService ;
    private static Logger logger = LogManager.getLogger(AppConfig.class.getPackage().getName());
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestHandlerInterceptor(userService,sessionDTO,env,institutionService));
        super.addInterceptors(registry);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**").addResourceLocations("/WEB-INF/resources/");
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    /**
     * Configure ViewResolvers to deliver preferred views.
     */
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        TilesViewResolver viewResolver = new TilesViewResolver();

        registry.viewResolver(viewResolver);
    }

    @Bean(name = "customAuthenticationProvider")
    public CustomAuthenticationProvider customAuthenticationProvider() {
        return new CustomAuthenticationProvider();
    }

    @Bean(name = "customAuthorizeRequests")
    public CustomAuthorizeRequests customAuthorizeRequests() {
        return new CustomAuthorizeRequests();
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

        //messageSource.setBasename("/WEB-INF/classes/*onfig*");
        messageSource.setBasenames("classpath:SystemConfig_en","classpath:config","classpath:MasterConfig_en","classpath:CardManagementConfig","classpath:CardSupportConfig","classpath:BulkProcessConfig_en");

        return messageSource;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public TilesConfigurer tilesConfigurer() {
        TilesConfigurer tilesConfigurer = new TilesConfigurer();

        tilesConfigurer.setDefinitions(new String[] { "/WEB-INF/resources/**/tiles.xml" });
        tilesConfigurer.setCheckRefresh(true);

        return tilesConfigurer;
    }


    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();

        viewResolver.setViewClass(JstlView.class);
        viewResolver.setPrefix("/WEB-INF/resources/jsp/pages/");
        viewResolver.setSuffix(".jsp");

        return viewResolver;
    }

    @Bean(name = "jdbcTemplate")
    @Autowired
    public JdbcTemplate getJdbcTemplate(@Qualifier("dataSource1") DataSource ds) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);

        return jdbcTemplate;
    }
    @Bean(name = "namedParameterJdbcTemplate")
    @Autowired
    public NamedParameterJdbcTemplate NamedParameterJdbcTemplate(@Qualifier("dataSource1") DataSource ds) {
    	NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(ds);

        return namedParameterJdbcTemplate;
    }
    @Bean(name = "multipartResolver")
    public CommonsMultipartResolver getResolver() throws IOException {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();

        // Set the maximum allowed size (in bytes) for each individual file.
        resolver.setMaxUploadSizePerFile(1000000000);    // 20MB
        resolver.setResolveLazily(true);

        // You may also set other available properties.

        return resolver;
    }
/*
    @Bean(name={"dataSource1"})
    public DataSource getJNDIDataSource(@Value("${jdbc.jndiName}") String jndiName)
      throws NamingException
    {
      JndiTemplate jndiTemplate = new JndiTemplate();
      DataSource dataSource = (DataSource)jndiTemplate.lookup(jndiName);
      return dataSource;
    }*/
    
    @Bean(name="dataSource1")
   	public DataSource itmDataSource(@Value("${itm.datasource.db.name}") String dbName,
   									@Value("${itm.datasource.url}") String serverName,
   									@Value("${itm.datasource.username}") String userName,
   									@Value("${itm.datasource.password}") String password,
   									@Value("${itm.datasource.db.libraries}") String libraries) {
   		AS400JDBCManagedConnectionPoolDataSource dbds = new AS400JDBCManagedConnectionPoolDataSource();
   		dbds.setDatabaseName(dbName);
   		dbds.setServerName(serverName);
   		dbds.setUser(userName);
   		dbds.setPassword(password);
   		dbds.setNaming(dbName);
   		dbds.setLibraries(libraries);

   		return dbds;
   	}
    /**
     * Configure Email Configuration
     * @return JavaMailSender
     */
    @Bean
    public JavaMailSender getMailSender(){
    	JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    	SpringMvcInitializer.SMSEMAIL_URL = env.getProperty("pcms.smsemail.utility.url");
		return mailSender;
    }
    
    /*
	 * FreeMarker configuration.
	 */
/*	@Bean
	public FreeMarkerConfigurationFactoryBean getFreeMarkerConfiguration() {
		FreeMarkerConfigurationFactoryBean bean = new FreeMarkerConfigurationFactoryBean();
		bean.setTemplateLoaderPath("/fmtemplates/");
		return bean;
	}*/
 
	@Bean
	public HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}
	
	@Bean
	@Scope(value=ConfigurableBeanFactory.SCOPE_SINGLETON)
	public AES initilizeAES(@Value("${pcms.aes.secretkey}") String key){
		
		AES  aes = new AES();
		try {
			aes.setKey(key);
		} catch (UnsupportedEncodingException|NoSuchAlgorithmException|NoSuchPaddingException  e) {
			logger.error("Exception in initilizeAES method",e);
		}
		return aes;
	}
	
	
	
	@Bean
	@Qualifier("emailExecutorService")
	public ExecutorService emailExecutorService(){
		 ExecutorService executor = Executors.newFixedThreadPool(Integer.parseInt(env.getProperty("pcms.email.thread.pool.size")));
		 return executor;
	}
	
	@Bean(name="messageSourceAccessor")
	public org.springframework.context.support.MessageSourceAccessor messageSourceAccessor(){
		 return new MessageSourceAccessor(messageSource());
	}
}

