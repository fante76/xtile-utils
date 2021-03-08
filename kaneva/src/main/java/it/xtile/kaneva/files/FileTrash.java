/**
 *
 * |\     /|
 * ( \   / )
 *  \ (_) /
 *   ) _ (
 *  / ( ) \
 * ( /   \ )
 * |/     \| T I L E
 *
 * ----------------------------
 * Implementa un cestino per file dove è possibile accodare i file da cancellare
 * subito o successivamente.
 *
 * Fornisce la possibilità di avviare e gestire un Thread per lo svuotamento periodico
 * della coda di file, provando sempre a cancellarli.
 *
 */
package it.xtile.kaneva.files;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements a trash for Files that cannot be immediately deleted for some
 * errors. Implements a mechanism to retry file deletion.
 *
 * @author luca
 *
 */
public class FileTrash {

	private static final Logger log = LogManager.getLogger(FileTrash.class);

	public static enum DELETE_RESULT {
		DELETED, QUEUED, FAILED
	};

	/**
	 * Max trash size that can be used to trigger a trash empty action.
	 */
	private int emptyTreshold;

	/**
	 * Max number of failed deletions on a file. Over that, file will be removed from the trash queue
	 */
	private int maxMissTimes;

	/**
	 * How much long a file can lay in the queue, before it is removed
	 */
	private Duration maxAge;

	/**
	 * The queue
	 */
	private List<FileEntry> trashedFiles;

	private final Predicate<FileEntry> isExpired = fe -> (maxMissTimes != 0 && fe.getMissTimes() > maxMissTimes)
			|| isDead(fe);

	public FileTrash() {
		this.trashedFiles = Collections.synchronizedList(new LinkedList<FileEntry>());
		this.emptyTreshold = 0;
		this.maxMissTimes = 0;
		this.maxAge = null;
	}

	/**
	 * Immediately deletes the given file. If failed, file will be queued for a future deletion
	 *
	 * It will check for file already queued
	 *
	 * @param file path to the file to be deleted. It it refers a directory, it must be already empty
	 */
	public void delete(Path file) {
		final Optional<FileEntry> fe = trashedFiles.stream().filter(f -> f.getPathToFile().equals(file)).findFirst();
		tryDelete(fe.orElse(new FileEntry(file)), fe.isPresent());
	}

	/**
	 * Recursively deletes a directory
	 *
	 * @param directory
	 */
	public void deletedTree(Path directory) {
		// TODO
	}

	/**
	 * It tries to delete a file from the filesystem. If it fails, the file will be queued on
	 * the trash for next time. Files already queued are checked for expiration: if
	 * expired they will be ejected from the queue and logs will happens for this.
	 *
	 * @param fe          Reference to the file
	 * @param fileTrashed True if file is already in queue, false otherwise
	 * @return True id deletion successed, false if queued or ejected
	 */
	private DELETE_RESULT tryDelete(FileEntry fe, boolean fileTrashed) {
		try {
			log.trace("Try to delete file {}", fe.getPathToFile());
			if (Files.exists(fe.getPathToFile())) {
				Files.delete(fe.getPathToFile());
			}
			trashedFiles.remove(fe);
			return DELETE_RESULT.DELETED;
		} catch (Exception e) {
			fe.addMissTime();
			if (isExpired.test(fe)) {
				log.warn("Finally failed to delete file {}. Entry removed, remove it manually. {}", fe.getPathToFile(),
						e.getMessage());
				trashedFiles.remove(fe);
				return DELETE_RESULT.FAILED;
			} else {
				log.trace("Error deleting file {}. File queued for next run. {}", fe.getPathToFile(), e.getMessage());
				if (!fileTrashed)
					trashedFiles.add(fe);
				return DELETE_RESULT.QUEUED;
			}
		}
	}

	/**
	 * Queues file for next deletion
	 *
	 * @param file
	 */
	public synchronized void add(Path file) {
		final FileEntry fe = new FileEntry(file);
		if (!trashedFiles.contains(fe)) {
			trashedFiles.add(fe);
			emptyIfOverTreshold();
		}
	}

	/**
	 * Run for delete! Try to remove from filesystem all queued file. For does who failed
	 * a missTimes will be added; other will stay in the queue.
	 *
	 * @return size of physically deleted files
	 */
	public int empty() {
		final AtomicInteger deletedSize = new AtomicInteger(0);
		// must use a new list to avoid concurrent modification
		new ArrayList<>(trashedFiles).stream().forEach(fe -> {
			if (tryDelete(fe, true) == DELETE_RESULT.DELETED)
				deletedSize.addAndGet(1);
		});
		return deletedSize.get();
	}

	public int emptyIfOverTreshold() {
		if (emptyTreshold > 0 && trashedFiles.size() > emptyTreshold)
			return empty();
		return 0;
	}

	public int removeWhatIsExpired() {
		final AtomicInteger deletedSize = new AtomicInteger(0);
		trashedFiles.stream().filter(isExpired).forEach(fe -> {
			try {
				Files.delete(fe.getPathToFile());
				deletedSize.addAndGet(1);
			} catch (Exception e) {
				log.warn("Finally failed to delete file {}", fe.getPathToFile());
			}
		});
		// remove anyway from queue
		trashedFiles.removeIf(isExpired);
		return deletedSize.get();
	}

	/**
	 * @return current trash size
	 */
	public int size() {
		return trashedFiles.size();
	}

	/**
	 * @return threshold form empty action
	 */
	public int getEmptyTreshold() {
		return emptyTreshold;
	}

	/**
	 * @param emptyTreshold threshold for empty action, compared with number of files in queue
	 */
	public void setEmptyTreshold(int emptyTreshold) {
		this.emptyTreshold = emptyTreshold;
	}

	public int getMaxMissTimes() {
		return maxMissTimes;
	}

	public void setMaxMissTimes(int maxMissTimes) {
		this.maxMissTimes = maxMissTimes;
	}

	public FileTrash withMaxMissTimes(int maxMissTimes) {
		this.maxMissTimes = maxMissTimes;
		return this;
	}

	public Duration getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(Duration maxAge) {
		this.maxAge = maxAge;
	}

	public FileTrash withMaxAge(Duration maxAge) {
		this.maxAge = maxAge;
		return this;
	}

	/**
	 *
	 * @param emptyTreshold threshold for empty action, compared with number of files in queue
	 * @return
	 */
	public FileTrash withEmptyTreshold(int emptyTreshold) {
		this.emptyTreshold = emptyTreshold;
		return this;
	}

	private boolean isDead(FileEntry fe) {
		long ageMS = System.currentTimeMillis() - fe.getAddTime().getTime();
		return maxAge != null && ageMS > maxAge.toMillis();
	}

	private class FileEntry {
		private Path pathToFile;
		private Date addTime;
		private int missTimes;

		public FileEntry(Path pathToFile) {
			addTime = new Date();
			missTimes = 0;
			this.pathToFile = pathToFile;
		}

		public int getMissTimes() {
			return missTimes;
		}

		public FileEntry addMissTime() {
			this.missTimes++;
			return this;
		}

		public Date getAddTime() {
			return this.addTime;
		}

		public Path getPathToFile() {
			return pathToFile;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getEnclosingInstance().hashCode();
			result = prime * result + ((pathToFile == null) ? 0 : pathToFile.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FileEntry other = (FileEntry) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
				return false;
			if (pathToFile == null) {
				if (other.pathToFile != null)
					return false;
			} else if (!pathToFile.equals(other.pathToFile))
				return false;
			return true;
		}

		private FileTrash getEnclosingInstance() {
			return FileTrash.this;
		}

	}

}
