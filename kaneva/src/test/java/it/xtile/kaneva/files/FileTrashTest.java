package it.xtile.kaneva.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

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
	@Disabled
	@DisplayName("Test delete of an existing file without problems")
	public void test001_deleteExistingFile() {
		final FileTrash trash = new FileTrash().withEmptyTreshold(100).withMaxAge(Duration.of(10, ChronoUnit.SECONDS))
				.withMaxMissTimes(2);
		try {
			final Path file = Files.createTempFile(jvmTmpDir, "toTrash", ".tmp");
			assertTrue(Files.exists(file), "File not created");
			trash.delete(file);
			assertEquals(0, trash.size(), "File queued but not deleted");
			assertFalse(Files.exists(file), "File still exists");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Disabled
	@Test
	@DisplayName("Test delete of an not existing file, simulating problems")
	public void test002_deleteNotExistingFile() {
		final FileTrash trash = new FileTrash().withEmptyTreshold(100).withMaxAge(Duration.of(10, ChronoUnit.SECONDS))
				.withMaxMissTimes(2);
		try {
			final Path dir = Files.createTempDirectory(jvmTmpDir, "ftt");
			final Path file = Files.createTempFile(dir, "toTrash", ".tmp");
			trash.delete(dir);
			assertEquals(1, trash.size(), "File deleted not queued");
			Files.delete(file);
			int deleted = trash.empty();
			assertEquals(1, deleted, "Queued file not deleted after empty");
			assertFalse(Files.exists(file), "File still exists");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	@DisplayName("Test delete expiring number of times")
	public void test003_expiringTimes() {
		final FileTrash trash = new FileTrash().withEmptyTreshold(100).withMaxAge(Duration.of(10, ChronoUnit.SECONDS))
				.withMaxMissTimes(2);
		try {
			final Path dir = Files.createTempDirectory(jvmTmpDir, "ftt");
			final Path file = Files.createTempFile(dir, "toTrash", ".tmp");
			for (int i = 0; i < 2; i++) {
				trash.delete(dir);
				assertEquals(1, trash.size(), "File deleted not queued");
			}
			trash.add(Files.createTempFile(jvmTmpDir, "toTrash2", ".tmp"));
			int deleted = trash.empty();
			assertEquals(1, deleted, "Queued file not deleted after empty");
			assertEquals(0, trash.size(), "Queued NOT empty");
			assertTrue(Files.exists(file), "First nested file deleted");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
