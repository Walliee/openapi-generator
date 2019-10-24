package org.openapitools.codegen.java;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.languages.JavaClientCodegen;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openapitools.codegen.languages.JavaClientCodegen.RESTTEMPLATE;

public class JavaRestTemplateClientCodegenTest {

	private RestTemplate restTemplate;
	private Object petApi;

	@BeforeTest
	public void setup() throws Exception {
		File sandBoxDir = new File("target/sandbox");
		if (sandBoxDir.exists()) {
			FileUtils.deleteDirectory(sandBoxDir);
		}
		sandBoxDir.mkdirs();

		Map<String, Object> properties = new HashMap<>();
		properties.put(JavaClientCodegen.JAVA8_MODE, true);
		properties.put(CodegenConstants.LIBRARY, RESTTEMPLATE);
		properties.put(CodegenConstants.API_PACKAGE, "xyz.abcdef.api");

		File output = Files.createTempDirectory("test").toFile();
		output.deleteOnExit();

		final CodegenConfigurator configurator = new CodegenConfigurator()
				.setGeneratorName("java")
				.setAdditionalProperties(properties)
				.setInputSpec("src/test/resources/3_0/issue_resttemplate_uri_variables.yaml")
				.setOutputDir(output.getAbsolutePath().replace("\\", "/"));

		final ClientOptInput clientOptInput = configurator.toClientOptInput();
		DefaultGenerator generator = new DefaultGenerator();
		generator.opts(clientOptInput).generate();

		List<File> paths = Files.walk(Paths.get(output.toURI()))
				.filter(path -> path.toUri().toString().endsWith(".java"))
				.filter(path -> path.toUri().toString().contains("main"))
				.map(Path::toFile)
				.collect(Collectors.toList());
		compileJavaFiles(paths, sandBoxDir);

		URLClassLoader classLoader = URLClassLoader
				.newInstance(new URL[] {sandBoxDir.toURI().toURL()});
		Class<?> apiClientClass = Class
				.forName("xyz.abcdef.ApiClient", true, classLoader);
		this.restTemplate = mock(RestTemplate.class);
		Object apiClient = apiClientClass.getConstructor(RestTemplate.class).newInstance(this.restTemplate);

		Class<?> petApiClass = Class.forName("xyz.abcdef.api.PetApi", true, classLoader);
		Constructor<?> petApiConstructor = petApiClass.getConstructor(apiClientClass);
		this.petApi = petApiConstructor.newInstance(apiClient);
	}

	@AfterTest
	public void teardown() {
		reset(this.restTemplate);
	}

	@Test
	public void testUriVariables_explodedQueryParams() throws Exception {

		ParameterizedTypeReference<List<Long>> returnType = new ParameterizedTypeReference<List<Long>>() {};
		when(this.restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(returnType), anyMap()))
				.thenReturn(ResponseEntity.noContent().build());

		Method findPetsByStatus = Stream.of(this.petApi.getClass().getMethods())
				.filter(m -> m.getName().equals("findPetsByStatus"))
				.findFirst()
				.get();

		Object findPetsByStatusInvocation = findPetsByStatus.invoke(this.petApi, Arrays.asList("available", "pending"));

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(org.springframework.http.MediaType.APPLICATION_JSON));
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
		headers.set("User-Agent", "Java-SDK");
		HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
		Map<String, String> uriVariables = new HashMap<>();
		uriVariables.put("status0", "available");
		uriVariables.put("status1", "pending");

		verify(this.restTemplate)
				.exchange("http://petstore.swagger.io/v2/pet/findByStatus?status={status0}&status={status1}",
						HttpMethod.GET, httpEntity, returnType, uriVariables);

	}

	@Test
	public void testUriVariables_commaSeparatedQueryParams() throws Exception {

		ParameterizedTypeReference<List<Long>> returnType = new ParameterizedTypeReference<List<Long>>() {};
		when(this.restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(returnType), anyMap()))
				.thenReturn(ResponseEntity.noContent().build());

		Method findPetsByTags = Stream.of(this.petApi.getClass().getMethods())
				.filter(m -> m.getName().equals("findPetsByTags"))
				.findFirst()
				.get();

		Object findPetsByTagsInvocation = findPetsByTags.invoke(this.petApi, Arrays.asList("1", "2"));

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(org.springframework.http.MediaType.APPLICATION_JSON));
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
		headers.set("User-Agent", "Java-SDK");
		HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
		Map<String, String> uriVariables = new HashMap<>();
		uriVariables.put("tags", "1,2");

		verify(this.restTemplate)
				.exchange("http://petstore.swagger.io/v2/pet/findByTags?tags={tags}",
						HttpMethod.GET, httpEntity, returnType, uriVariables);

	}

	@Test
	public void testUriVariables_pathParam() throws Exception {

		ParameterizedTypeReference<Void> returnType = new ParameterizedTypeReference<Void>() {};
		when(this.restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(returnType), anyMap()))
				.thenReturn(ResponseEntity.noContent().build());

		Method deletePet = Stream.of(this.petApi.getClass().getMethods())
				.filter(m -> m.getName().equals("deletePet"))
				.findFirst()
				.get();

		Object deletePetInvocation = deletePet.invoke(this.petApi, 1L, null);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
		headers.set("User-Agent", "Java-SDK");
		HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
		Map<String, Object> uriVariables = new HashMap<>();
		uriVariables.put("petId", 1L);

		verify(this.restTemplate)
				.exchange("http://petstore.swagger.io/v2/pet/{petId}",
						HttpMethod.DELETE, httpEntity, returnType, uriVariables);
	}

	@Test
	public void testUriVariables_pathAndQueryParam() throws Exception {

		ParameterizedTypeReference<Void> returnType = new ParameterizedTypeReference<Void>() {};
		when(this.restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(returnType), anyMap()))
				.thenReturn(ResponseEntity.noContent().build());

		Method deletePet = Stream.of(this.petApi.getClass().getMethods())
				.filter(m -> m.getName().equals("deletePet"))
				.findFirst()
				.get();

		Object deletePetInvocation = deletePet.invoke(this.petApi, 1L, "sample_api_key");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
		headers.set("User-Agent", "Java-SDK");
		HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
		Map<String, Object> uriVariables = new HashMap<>();
		uriVariables.put("petId", 1L);
		uriVariables.put("api_key", "sample_api_key");

		verify(this.restTemplate)
				.exchange("http://petstore.swagger.io/v2/pet/{petId}?api_key={api_key}",
						HttpMethod.DELETE, httpEntity, returnType, uriVariables);
	}

	private static void compileJavaFiles(List<File> testSourceFileNames, File destinationDir) {
		// Get an instance of java compiler
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		// Get a new instance of the standard file manager implementation
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

		Iterable<? extends JavaFileObject> compilationUnits1 = fileManager.getJavaFileObjectsFromFiles(testSourceFileNames);

		// Create the compilation task
		List<String> options = new ArrayList<>(Arrays.asList("-d", destinationDir.getAbsolutePath()));
		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, compilationUnits1);

		task.call();
	}
}
