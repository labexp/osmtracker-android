package me.guillaumin.android.osmtracker.osm;

public class OSMConstants {

	private static final String OSM_API_URL = "http://www.openstreetmap.org";
	
	public static class Api {
		private static final String PATH = "/api/0.6";
		
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
	
	public static class OAuth {
		public static final String CONSUMER_KEY = "1eYt7J6qLY858GaHdRv2CeTuKyl1pvr9tC2lI6Zz";
		public static final String CONSUMER_SECRET = "yL0NNWXXz9ZZUwQlFhiuuLWKzrdjWu7H00KkXTrr";
		
		public static class Urls {
			public static final String REQUEST_TOKEN_URL = OSM_API_URL + "/oauth/request_token";
			public static final String ACCESS_TOKEN_URL = OSM_API_URL + "/oauth/access_token";
			public static final String AUTHORIZE_TOKEN_URL = OSM_API_URL + "/oauth/authorize";
		}
			
	}
	
}
