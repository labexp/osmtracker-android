package net.osmtracker.osm;

public class OpenStreetMapConstants {

	private static final boolean DEV_MODE = true;
	private static final String OSM_API_URL_DEV = "https://master.apis.dev.openstreetmap.org";
	private static final String OSM_API_URL_PROD = "https://www.openstreetmap.org";
	private static final String OSM_API_URL = (DEV_MODE) ? OSM_API_URL_DEV : OSM_API_URL_PROD;
	
	public static class Api {

		private static final String PATH = "/api/0.6";

		public static final String OSM_API_URL_PATH = OSM_API_URL + PATH + "/";
		
		public static class Gpx {
			public static final String CREATE = OSM_API_URL + PATH + "/gpx/create";
			
			public static class Parameters {
				public static final String FILE = "file";
				public static final String DESCRIPTION = "description";
				public static final String TAGS = "tags";
				public static final String VISIBILITY = "visibility";
			}
		}
	}

	public static class OAuth2 {
		public static final String CLIENT_ID_PROD = ""; // TODO: agregar el client ID para producción.
		public static final String CLIENT_ID_DEV = "94Ht-oVBJ2spydzfk18s1RV2z7NS98SBwMfzSCqLQLE"; // DEV

		public static final String CLIENT_ID = (DEV_MODE) ? CLIENT_ID_DEV : CLIENT_ID_PROD;

		public static final String CLIENT_SECRET_PROD = ""; // TODO: agregar el client ID para producción.
		public static final String CLIENT_SECRET_DEV = "yL0NNWXXz9ZZUwQlFhiuuLWKzrdjWu7H00KkXTrr";
		public static final String CLIENT_SECRET = (DEV_MODE) ? CLIENT_SECRET_DEV : CLIENT_SECRET_PROD;

		public static final String SCOPE = "write_gpx";
		public static final String USER_AGENT = "OSMTracker for Android™";

		public static class Urls {
			public static final String AUTHORIZATION_ENDPOINT = OSM_API_URL + "/oauth2/authorize";
			public static final String TOKEN_ENDPOINT = OSM_API_URL + "/oauth2/token";
		}

	}

	
}
