package it.xtile.kaneva.files;

import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;

@TestMethodOrder(MethodName.class)
public class FinderTest {
	
	@Test
	public void test001_linuxPath() {
		final String[] texts = {
				"My personal directory is /root anyway", 
				"No Files found in: /root/documents",
				"Error: root/documents not found", 
				"File [root/docum_123/aa!!/aaa.txt] does not exists",
				"Failed replace /root/${name} with aaa/bbb"
		};
		final String[] wanted = {"/root","/root/documents","[root/docum_123/aa!!/aaa.txt]","/root/${name}"};
		System.setProperty("os.name", "Linux");
		List<String> found = new ArrayList<>();
		Stream.of(texts).forEach(t->found.addAll(Finder.pathsInText(t)));
		assertEquals(4, found.size(), "Found different amounts of paths");
		for(int i=0; i<wanted.length; i++) {
			assertEquals(wanted[i], found.get(i), "Percorso non trovato");
		}
	}
	
	@Test
	@Disabled
	//"need to find a way to simulate Windows system anyway"
	private void test002_windowsPath() {
		final String[] texts = {
				"My personal directory is c:\\ anyway", 
				"No Files found in: e:\\main\\documents",
				"Error: main\\documents not found", 
				"File [c:\\main\\docum_123\\aa!!\\aaa.txt] does not exists",
				"Failed replace \\\\root\\${name} with aaa\\bbb"
		};
		final String[] wanted = {"c:\\","e:\\main\\documents","[c:\\main\\docum_123\\aa!!\\aaa.txt]","\\\\root\\${name}"};
		System.setProperty("os.name", "Windows10");
		List<String> found = new ArrayList<>();
		Stream.of(texts).forEach(t->found.addAll(Finder.pathsInText(t)));
		assertEquals(4, found.size(), "Found different amounts of paths");
		for(int i=0; i<wanted.length; i++) {
			assertEquals(wanted[i], found.get(i), "Percorso non trovato");
		}
	}

}
