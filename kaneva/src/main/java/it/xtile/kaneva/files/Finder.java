/**
 * Search of files and paths in many context
 */
package it.xtile.kaneva.files;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import it.xtile.kaneva.utils.OperatingSystem;


public class Finder {
	
	private static final Pattern linuxPathPattern = Pattern.compile("(/.+)|(.*/.*/.*)");
	private static final Pattern windowsPathPattern = Pattern.compile("([a-zA-Z]:[\\\\].*)|([\\\\][\\\\].+)|(.*[\\\\].*[\\\\].*)");
	private static final Predicate<String> isLinuxPathPredicate = s -> linuxPathPattern.matcher(s).matches();
	private static final Predicate<String> isWindowsPathPredicate = s -> windowsPathPattern.matcher(s).matches();
	
	public static final List<String> pathsInText(String text) {
		if (StringUtils.isBlank(text)) return Collections.emptyList();
		final String[] tokens = StringUtils.split(text, ' ');
		if (OperatingSystem.isUnix() || OperatingSystem.isMac())
			return Stream.of(tokens).filter(isLinuxPathPredicate).collect(Collectors.toList());
		return Stream.of(tokens).filter(isWindowsPathPredicate).collect(Collectors.toList());
	}

}
