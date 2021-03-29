package it.xtile.kaneva.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodName.class)
public class FileTrashTest {

	private static Path jvmTmpDir;

	@BeforeAll
	public static void init() {
		jvmTmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
	}

	@Test
	@DisplayName("Test delete of an existing file without problems")
	public void test001_deleteExistingFile() throws IOException {
		final FileTrash trash = new FileTrash().withEmptyTreshold(100).withMaxAge(Duration.of(10, ChronoUnit.SECONDS))
				.withMaxMissTimes(2);
		Path file=null;
		try {
			file = Files.createTempFile(jvmTmpDir, "toTrash", ".tmp");
			assertTrue(Files.exists(file), "File not created");
			trash.delete(file);
			assertEquals(0, trash.size(), "File queued but not deleted");
			assertFalse(Files.exists(file), "File still exists");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (file!=null) Files.deleteIfExists(file);
		}
	}

	@Test
	@DisplayName("Test delete of a not existing file, simulating problems and testing queue")
	public void test002_deleteNotExistingFile() throws IOException {
		final FileTrash trash = new FileTrash().withEmptyTreshold(100).withMaxAge(Duration.of(10, ChronoUnit.SECONDS))
				.withMaxMissTimes(2);
		Path dir=null;
		Path file=null;
		try {
			dir = Files.createTempDirectory(jvmTmpDir, "ftt");
			file = Files.createTempFile(dir, "toTrash", ".tmp");
			trash.delete(dir);
			assertEquals(1, trash.size(), "File deleted not queued");
			Files.delete(file);
			int deleted = trash.empty();
			assertEquals(1, deleted, "Queued file not deleted after empty");
			assertFalse(Files.exists(file), "File still exists");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (file!=null) Files.deleteIfExists(file);
			if (dir!=null) Files.deleteIfExists(dir);
		}
	}

	@Test
	@DisplayName("Test delete expiring number of times")
	public void test003_expiringTimes() throws IOException {
		final FileTrash trash = new FileTrash().withEmptyTreshold(100).withMaxAge(Duration.of(10, ChronoUnit.SECONDS))
				.withMaxMissTimes(3);
		Path dir=null;
		Path file=null;
		try {
			dir = Files.createTempDirectory(jvmTmpDir, "ftt");
			file = Files.createTempFile(dir, "toTrash", ".tmp");
			// check queuing and duplicates management
			for (int i = 0; i < 2; i++) {
				// this will fail because dir is not empty
				trash.delete(dir); //first miss
				assertEquals(1, trash.size(), "File deleted not queued");
			}
			trash.add(Files.createTempFile(jvmTmpDir, "toTrash2", ".tmp"));
			int deleted = trash.empty(); //second miss
			assertEquals(1, deleted, "Queued file not deleted after empty");
			// one deleted, one still on queue
			assertEquals(1, trash.size(), "Queued empty");
			deleted = trash.empty(); // third miss
			// last one rejected for miss overdue
			assertEquals(0, trash.size(), "Queued NOT empty");
			assertTrue(Files.exists(file), "First nested file wrongly deleted");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (file!=null) Files.deleteIfExists(file);
			if (dir!=null) Files.deleteIfExists(dir);
		}

	}
	
	@Test
	@DisplayName("Test recursively delete a directory")
	public void test004_deleteFolder() throws IOException {
		final Path rootDir = Files.createTempDirectory("FileTrash");
		final String tmpDirPlain = rootDir.toAbsolutePath().toString();
		List<String> paths = new ArrayList<>();
		Collections.addAll(paths, tmpDirPlain+File.separator+"dir001",
				tmpDirPlain+File.separator+"dir001"+File.separator+"file001.txt",
				tmpDirPlain+File.separator+"dir001"+File.separator+"file002.txt",
				tmpDirPlain+File.separator+"dir002",
				tmpDirPlain+File.separator+"dir003",
				tmpDirPlain+File.separator+"dir003"+File.separator+"dir0031",
				tmpDirPlain+File.separator+"dir003"+File.separator+"dir0032",
				tmpDirPlain+File.separator+"dir003"+File.separator+"dir0032"+File.separator+"file003.txt");
		for(String s : paths) {
			Path path = Paths.get(s);
			if (s.endsWith(".txt")) {
				Files.createFile(path);
			} else 
				Files.createDirectory(path);
		}
		
		assertTrue(Files.exists(rootDir), "Main directory doesn't exists");
		assertEquals(3, Files.list(rootDir).count(), "Directory tree failed to create");
		
		final FileTrash trash = new FileTrash().withEmptyTreshold(100).withMaxAge(Duration.of(10, ChronoUnit.SECONDS))
				.withMaxMissTimes(2);

		trash.deleteTree(rootDir);
		assertFalse(Files.exists(rootDir), "Directory still exists");
	}
	
	/*
	 * TODO check concurrency modifications of the trash with same sub-test executed
	 * on different Threads. Sub-test must have a well defined result to be tested
	 */

}
