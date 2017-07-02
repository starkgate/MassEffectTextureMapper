import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.query.option.QueryOptions;

public class Texture {
	private int groupId;
	private String game;
	private String name;
	private String crc;
	private String textureClass;
	private String author;

	public Texture(int groupId, String game, String crc, String textureClass) {
		this.groupId = groupId;
		this.game = game;
		this.crc = crc;
		this.textureClass = textureClass;
	}
	
	public Texture(String crc, String authorOrName, boolean isAuthor) {
		if(isAuthor){
			this.author = authorOrName;
			this.crc = crc;
		} else {
			this.name = authorOrName;
			this.crc = crc;
		}
	}
	
	public static final Attribute<Texture, String> NAME = new SimpleAttribute<Texture, String>("name") {

		@Override
		public String getValue(Texture texture, QueryOptions arg1) {
			return texture.name;
		}
	};
	
	public static final Attribute<Texture, String> AUTHOR = new SimpleAttribute<Texture, String>("author") {

		@Override
		public String getValue(Texture texture, QueryOptions arg1) {
			return texture.author;
		}
	};
	
	public static final Attribute<Texture, Integer> GROUP_ID = new SimpleAttribute<Texture, Integer>("groupId") {

		@Override
		public Integer getValue(Texture texture, QueryOptions arg1) {
			return texture.groupId;
		}
	};
	
	public static final Attribute<Texture, String> GAME = new SimpleAttribute<Texture, String>("game") {

		@Override
		public String getValue(Texture texture, QueryOptions arg1) {
			return texture.game;
		}
	};
	
	public static final Attribute<Texture, String> CRC = new SimpleAttribute<Texture, String>("crc") {

		@Override
		public String getValue(Texture texture, QueryOptions arg1) {
			return texture.crc;
		}
	};
	
	public boolean equals(Texture texture) {
		return this.crc.equals(texture.crc);
	}
	
	public String toString() {
		return (this.crc + ".dds").toString();
	}

	public int getGroupId() {
		return groupId;
	}

	public String getGame() {
		return game;
	}

	public String getCrc() {
		return crc;
	}
	
	public String getAuthor() {
		return author;
	}
	
	public String getTextureClass() {
		return textureClass;
	}
}
