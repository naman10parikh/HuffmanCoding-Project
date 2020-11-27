import java.util.PriorityQueue;

 /*
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){

		int[] count = new int[1 + ALPH_SIZE];
		int checkbit = in.readBits(BITS_PER_WORD);
		while (checkbit != -1)
		{
			count[checkbit]+=1;
			checkbit = in.readBits(BITS_PER_WORD);
		}
		count[PSEUDO_EOF] = 1;
		PriorityQueue<HuffNode> pq1 = new PriorityQueue<>();
		for (int k = 0; k < count.length; k+=1)
		{
			if (count[k] > 0)
			{
				pq1.add(new HuffNode(k, count[k], null, null));
			}
		}
		while (pq1.size() > 1)
		{
			HuffNode leftnode = pq1.remove();
			HuffNode rightnode = pq1.remove();
			HuffNode treeupdate = new HuffNode(0, leftnode.myWeight + rightnode.myWeight, leftnode, rightnode);
			pq1.add(treeupdate);
		}
		HuffNode hn = pq1.remove();
		String[] encode = new String[1 + ALPH_SIZE];
		treecoding(hn, encode, "");
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		header(hn, out);
		in.reset();
		while (true)
		{
			int bitupdate = in.readBits(BITS_PER_WORD);
			if (bitupdate == -1)
			{
				break;
			}
			String code = encode[bitupdate];
			if (code != null)
			{
				out.writeBits(code.length(), Integer.parseInt(code, 2));
			}
		}
		String pstr = encode[PSEUDO_EOF];
		out.writeBits(pstr.length(), Integer.parseInt(pstr, 2));
		out.close();
	}
	private void treecoding(HuffNode hn, String[] pstr, String s)
	{
		if (hn.myRight == null && hn.myLeft == null)
		{
			pstr[hn.myValue] = s;
			return;
		}
		treecoding(hn.myLeft, pstr, s + "0");
		treecoding(hn.myRight, pstr, s + "1");
	}

	private void header(HuffNode hn, BitOutputStream out)
	{
		if (hn.myRight != null || hn.myLeft != null) {
			out.writeBits(1, 0);
			header(hn.myLeft, out);
			header(hn.myRight, out);
		}
		else
		{
			out.writeBits(1, 1);
			out.writeBits(1 + BITS_PER_WORD, hn.myValue);
		}
	}
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out)
	{
		int magic = in.readBits(BITS_PER_INT);
		if (magic != HUFF_TREE) {
			throw new HuffException("invalid magic number "+magic);
		}
		HuffNode nodehead = readheader(in);
		HuffNode nodetracker = nodehead;
		while (true)
		{
			int newbits = in.readBits(1);
			if (newbits == -1)
			{
				throw new HuffException("Failure in reading bits");
			}
			else
			{
				if (newbits == 0)
				{
					nodetracker = nodetracker.myLeft;
				}
				else
				{
					nodetracker = nodetracker.myRight;
				}
				if (nodetracker.myLeft == null && nodetracker.myRight == null)
				{
					if (nodetracker.myValue == PSEUDO_EOF)
					{
						break;
					}
					else
					{
						out.writeBits(BITS_PER_WORD, nodetracker.myValue);
						nodetracker = nodehead;
					}
				}
			}
		}
		out.close();
	}
	private HuffNode readheader(BitInputStream in)
	{
		int bits = in.readBits(1);
		if (bits == -1)
		{
			throw new HuffException("Failure in reading bits");
		}
		if (bits == 0)
		{
			HuffNode leftnode = readheader(in);
			HuffNode rightnode = readheader(in);
			return new HuffNode(0, 0, leftnode, rightnode);
		}
		else
		{
			return new HuffNode((in.readBits(1 + BITS_PER_WORD)), 0, null, null);
		}
	}
}
