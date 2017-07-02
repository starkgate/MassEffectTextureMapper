import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.query.Query;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import static com.googlecode.cqengine.query.QueryFactory.*;

public class Database extends ConcurrentIndexedCollection<Texture> {
	private IndexedCollection<Texture> database;
	private IndexedCollection<Texture> ME1_Tree;
	private IndexedCollection<Texture> ME2_Tree;
	private IndexedCollection<Texture> ME3_Tree;
	private IndexedCollection<Texture> ME2_Authors;
	private IndexedCollection<Texture> ME3_Authors;

	public Database(List<List<Object>> textureMapValues, List<List<Object>> textureAuthorsME2,
			List<List<Object>> textureAuthorsME3) {
		database = createDatabase(textureMapValues);
		ME2_Authors = createAuthors(textureAuthorsME2);
		ME3_Authors = createAuthors(textureAuthorsME3);
		for (int i = 1; i <= 3; i++)
			setupTree(i);
	}

	public void setupTree(int game) {
		String csv_name = "ME" + game + "_Tree.csv";

		List<String[]> ME = null;
		try {
			CsvParserSettings settings = new CsvParserSettings();
			settings.getFormat().setLineSeparator("\n");
			CsvParser ME_csv = new CsvParser(settings);
			ME = ME_csv.parseAll(new FileReader(csv_name));
		} catch (IOException e) { // if csv does not exist or can't be read
			e.printStackTrace();
		}

		switch (game) {
		case 1:
			ME1_Tree = createTree(ME, game);
			break;
		case 2:
			ME2_Tree = createTree(ME, game);
			break;
		case 3:
			ME3_Tree = createTree(ME, game);
			break;
		}
	}

	public ConcurrentIndexedCollection<Texture> createTree(List<String[]> values, int game) {
		ConcurrentIndexedCollection<Texture> tree = new ConcurrentIndexedCollection<>();

		if (values == null || values.size() == 0) {
			System.out.println("Table is empty.");
		} else {
			for (String[] row : values) {
				if (row.length != 0) {
					String crc = row[0]; // 0x12345678
					String name = row[1];
					tree.add(new Texture(crc, name, false));
				}
			}
		}
		tree.addIndex(HashIndex.onAttribute(Texture.CRC));
		return tree;
	}

	public ConcurrentIndexedCollection<Texture> createAuthors(List<List<Object>> values) {
		ConcurrentIndexedCollection<Texture> tree = new ConcurrentIndexedCollection<>();

		if (values == null || values.size() == 0) {
			System.out.println("Table is empty.");
		} else {
			for (int i = 0; i < values.size(); i++) {
				List<Object> row = values.get(i);
				if (row.size() != 0) {
					String crc = row.get(0).toString();
					String author = row.get(1).toString();
					tree.add(new Texture(crc, author, true));
				}
			}
		}
		tree.addIndex(HashIndex.onAttribute(Texture.AUTHOR));
		return tree;
	}

	public ConcurrentIndexedCollection<Texture> createDatabase(List<List<Object>> values) {
		ConcurrentIndexedCollection<Texture> database = new ConcurrentIndexedCollection<>();

		if (values == null || values.size() == 0) {
			System.out.println("Table is empty.");
		} else {
			for (int i = 0; i < values.size(); i++) {
				List<Object> row = values.get(i);
				if (row.size() != 0) {
					int groupId = Integer.parseInt(row.get(0).toString()); // 8005
					String game = row.get(1).toString(); // ME1
					String crc = row.get(2).toString(); // 0x12345678
					String textureClass = row.get(10).toString();

					database.add(new Texture(groupId, game, crc, textureClass));
				} else {
					System.out.println("Row " + (i + 2) + " is empty.");
				}
			}
		}
		database.addIndex(HashIndex.onAttribute(Texture.CRC));
		database.addIndex(HashIndex.onAttribute(Texture.GROUP_ID));
		return database;
	}

	// Find all duplicates from one texture
	public List<List<String>> search(String crc, String game, boolean portSoloTextures, boolean matchAuthor) {
		List<List<String>> result = new ArrayList<>();
		Texture subresult = null;
		Query<Texture> query_CRC = equal(Texture.CRC, crc); // find texture
		String author = findAuthor(game, query_CRC);

		try { // texture in database ?
			subresult = database.retrieve(query_CRC).iterator().next();

			int groupId = subresult.getGroupId(); // finds the groupId of the
													// texture, if texture is
													// present
			Query<Texture> query_groupId = equal(Texture.GROUP_ID, groupId); // find
																				// texture

			for (Texture t : database.retrieve(query_groupId)) {
				if (t.getGame().equals(game)) {
					List<String> row = new ArrayList<>();
					row.add(t.getCrc());
					row.add(t.getTextureClass());

					if (matchAuthor) {
						row.add(author);
					}

					result.add(row);
				}
			}
		} catch (Exception e) { // if texture not in database
			try { // texture in tree ?
				if (portSoloTextures) { // if option to copy all matching
										// textures is selected
					switch (game) {
					case "ME1":
						subresult = ME1_Tree.retrieve(query_CRC).iterator().next();
						break;
					case "ME2":
						subresult = ME2_Tree.retrieve(query_CRC).iterator().next();
						break;
					case "ME3":
						subresult = ME3_Tree.retrieve(query_CRC).iterator().next();
						break;
					}

					List<String> row = new ArrayList<>();
					row.add(subresult.getCrc());
					row.add("solo"); // not a duplicate

					if (matchAuthor) {
						row.add(author);
					}

					result.add(row);
				}
			} catch (Exception f) {
				return null; // if texture not in tree
			}
		}
		return result;
	}

	private String findAuthor(String game, Query<Texture> query_CRC) {
		String author = "";

		try { // is there an author ?
			switch (game) {
			case "ME2":
				author = ME2_Authors.retrieve(query_CRC).iterator().next().getAuthor();
				break;
			case "ME3":
				author = ME3_Authors.retrieve(query_CRC).iterator().next().getAuthor();
				break;
			}
		} catch (Exception g) {
		}
		return author;
	}
}
