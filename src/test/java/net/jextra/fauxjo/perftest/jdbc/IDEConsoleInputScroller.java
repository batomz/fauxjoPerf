package net.jextra.fauxjo.perftest.jdbc;

import java.io.CharArrayWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Hack workaround for headless (no GUI) testing because java cannot mask passwords in IDE consoles as of 2021!.
 * <p>
 * EXPERIMENTAL: DO NOT USE IN PRODUCTION
 * ONLY use for local database testing because this is not a secure solution.
 * Consoles honoring ANSI chars will be cleared then scrolled with newlines. ANSI clear codes
 * and newlines are written to standard out after every [ENTER] key press which scrolls input off the
 * screen making it slightly difficult for an onlooker to see the password. Prompts the user to input
 * a password one character at a time. Can be configured to ignore all but the last character to discourage
 * single-line input.
 */
public class IDEConsoleInputScroller {

	/**
	 * Return sequence of chars entered by the user until a forward slash is entered.
	 * <p>
	 * If strict is true, only captures the last character of a sequence entered to discourage single-line
	 * input whereby an onlooker might easily see the password. Capture terminates when the forward slash
	 * character is entered or a sequence ends with it.
	 * @param strict is true only the last character entered is captured.
	 */
	public static String getIDEConsoleInput(boolean strict) throws Exception {
		StringBuilder tmp = new StringBuilder("\033[H\033[2J"); //ANSI clear for non-iDE console
		for(int i=0;i<384;i++) { tmp.append('\n'); }
		byte[] MASK = tmp.toString().getBytes();

		System.out.println("Input the DB password. Pressing [ENTER] after EACH ");
		System.out.println("character " + (strict ? "is REQUIRED" : "will mask input") + ". Type / [ENTER] to Connect.");
		System.out.println("+--------------------------------------------");
		int maxPwCharLen = 20;
		CharArrayWriter caw = null;
		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(System.in,  StandardCharsets.UTF_8);
			caw = new CharArrayWriter();
			char[] buf = new char[128];
			int r = 0;
			for (int i = 0; i < maxPwCharLen; i++) {
				if ((r = isr.read(buf)) < 1) {
					continue;
				}
				System.out.write(MASK);
		    System.out.flush();
				 //Console always returns ENTER so input will always have at least 2 characters.
				if(r < 2) continue;
				if(buf[r-2] == '/') {
					if(!strict) {
						caw.write(buf, 0, r - 2);
					} else if(r > 2) {
						caw.write(buf[r - 3]);
					}
					break;
				} else if(!strict) {
					caw.write(buf, 0, r - 1);
				} else {
					caw.write(buf[r - 2]);
				}
			}
		} finally {
			if(caw != null) {
				caw.flush();
				caw.close();
			}
		}
		return caw.toString();
	}

	public static void main(String[] args) {
		try {
			String pw = IDEConsoleInputScroller.getIDEConsoleInput(true);
			System.out.println("pw: " + pw);
		} catch(Exception x) {
			x.printStackTrace();
		}
	}
}
