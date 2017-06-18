import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import com.googlecode.cqengine.lib.com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.lib.com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.lib.com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.lib.com.googlecode.cqengine.query.Query;
import com.opencsv.CSVReader;

import static com.googlecode.cqengine.lib.com.googlecode.cqengine.query.QueryFactory.*;

public class Database extends ConcurrentIndexedCollection<Texture> {
	private IndexedCollection<Texture> database;
	private IndexedCollection<Texture> ME1_Tree;
	private IndexedCollection<Texture> ME2_Tree;
	private IndexedCollection<Texture> ME3_Tree;

	public Database(List<List<Object>> values) {
		database = createDatabase(values);

		try {
			CSVReader ME1_csv;
			ME1_csv = new CSVReader(new FileReader("ME1_Tree.csv"), ',');
			List<String[]> ME1 = ME1_csv.readAll();
			ME1_csv.close();
			ME1_Tree = createTree(ME1);

			CSVReader ME2_csv = new CSVReader(new FileReader("ME2_Tree.csv"), ',');
			List<String[]> ME2 = ME2_csv.readAll();
			ME2_csv.close();
			ME2_Tree = createTree(ME2);

			CSVReader ME3_csv = new CSVReader(new FileReader("ME3_Tree.csv"), ',');
			List<String[]> ME3 = ME3_csv.readAll();
			ME3_csv.close();
			ME3_Tree = createTree(ME3);
		} catch (Exception e) {}
	}

	public ConcurrentIndexedCollection<Texture> createTree(List<String[]> values) {
		ConcurrentIndexedCollection<Texture> tree = new ConcurrentIndexedCollection<>();

		if (values == null || values.size() == 0) {
			System.out.println("Table is empty.");
		} else {
			for (String[] row : values) {
				if(row.length != 0) {
					String crc = row[0]; // 0x12345678
					String name = row[1];
					tree.add(new Texture(crc, name));
				}
			}
		}
		tree.addIndex(HashIndex.onAttribute(Texture.NAME));
		return tree;
	}

	public ConcurrentIndexedCollection<Texture> createDatabase(List<List<Object>> values) {
		ConcurrentIndexedCollection<Texture> database = new ConcurrentIndexedCollection<>();

		if (values == null || values.size() == 0) {
			System.out.println("Table is empty.");
		} else {
			for (int i = 0; i < values.size(); i++) {
				List<Object> row = values.get(i);
				if(row.size() != 0) {
					int groupId = Integer.parseInt(row.get(0).toString()); // 8005
					String game = row.get(1).toString(); // ME1
					String crc = row.get(2).toString(); // 0x12345678
					String textureClass = row.get(10).toString();

					database.add(new Texture(groupId, game, crc, textureClass));
				} else {
					System.out.println("Row " + (i+2) + " is empty.");
				}
			}
		}
		database.addIndex(HashIndex.onAttribute(Texture.CRC));
		database.addIndex(HashIndex.onAttribute(Texture.GROUP_ID));
		return database;
	}

	public List<List<String>> search(String crc, String game, boolean portSoloTextures) {
		List<List<String>> result = new ArrayList<>();
		Texture subresult = null;
		Query<Texture> query_CRC = equal(Texture.CRC, crc); // find texture

		try { // texture in database ?
			subresult = database.retrieve(query_CRC).iterator().next();

			int groupId = subresult.getGroupId(); // finds the groupId of the texture, if texture is present
			Query<Texture> query_groupId = equal(Texture.GROUP_ID, groupId); // find texture

			for(Texture t : database.retrieve(query_groupId)){
				if(t.getGame().equals(game)){
					List<String> row = new ArrayList<>();
					row.add(t.getCrc());
					row.add(t.getTextureClass());
					result.add(row);
				}
			}
		} catch(Exception e) { // if texture not in database
			try { // texture in tree ?
				if(portSoloTextures) { // if option to copy all matching textures is selected
					switch(game){
					case "ME1":
						subresult = ME1_Tree.retrieve(query_CRC).iterator().next();
					case "ME2":
						subresult = ME2_Tree.retrieve(query_CRC).iterator().next();
					case "ME3":
						subresult = ME3_Tree.retrieve(query_CRC).iterator().next();
					}

					List<String> row = new ArrayList<>();
					row.add(subresult.getCrc());
					row.add("solo"); // not a duplicate
					result.add(row);
				}
			} catch(Exception f){
				return null; // if texture not in tree
			}
		}

		return result;
	}
}
