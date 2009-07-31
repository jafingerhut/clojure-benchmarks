// Purpose: Contrast library LinkedList with the manual solutions.
//          The link management infrastructure is completely hidden.
// Author : Fred Swartz, 21 Feb 2006, placed in the public domain.

import java.io.*;
import java.util.*;

public class revlines {
    public static void main(String[] args) { 
	BufferedReader br =
	    new BufferedReader(new InputStreamReader(System.in));
	BufferedWriter bw =
	    new BufferedWriter(new OutputStreamWriter(System.out));

	// Read all lines from standard input, saving them in a linked
	// list of Strings lst.
        LinkedList<String> lst = new LinkedList<String>();
	while (true) {
	    try {
		String ln = br.readLine();
		if (ln == null) {
		    break;
		}
		lst.add(ln);
	    } catch (IOException ioe) {
		System.out.println("IO error trying to read line from standard input");
		System.exit(1);
	    }
	}
        
	// Create a new linked list that is the reverse of the first.
	LinkedList<String> reversed_lst = new LinkedList<String>();
	for (String s : lst) {
	    reversed_lst.addFirst(s);
	}

	// Iterate through the reversed list, printing the lines as we go.
        for (String s : reversed_lst) {
	    try {
		bw.write(s.toCharArray(), 0, s.length());
		bw.newLine();
	    } catch (IOException ioe) {
		System.out.println("IO error trying to write line to standard output");
		System.exit(1);
	    }
        }
	try {
	    bw.flush();
	} catch (IOException ioe) {
	    System.out.println("IO error trying to flush to standard output");
	    System.exit(1);
	}
    }
}
