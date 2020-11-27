 /*
 *	Used in place of IOException so user does not have to use
 *	try-catch blocks.  Used in place of RuntimeException so
 *	that regular RuntimeExceptions are still handled in the regular
 *	way.
 */

@SuppressWarnings("serial")
public class HuffException extends RuntimeException {

	public HuffException(String error) {
		super(error);
	}
}
