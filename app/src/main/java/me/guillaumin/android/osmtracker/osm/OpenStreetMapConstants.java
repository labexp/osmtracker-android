package me.guillaumin.android.osmtracker.osm;

public class OpenStreetMapConstants {

	private static final boolean DEV_MODE = false;
	
	private static final String OSM_API_URL_DEV = "http://master.apis.dev.openstreetmap.org";
	private static final String OSM_API_URL_PROD = "http://www.openstreetmap.org";
	private static final String OSM_API_URL = (DEV_MODE) ? OSM_API_URL_DEV : OSM_API_URL_PROD;
	
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
		public static final String CONSUMER_KEY_DEV = "FgGrirJh5UORShsKWiElkSQuN54DRoJ21UgwnrFW";
		public static final String CONSUMER_KEY_PROD = "1eYt7J6qLY858GaHdRv2CeTuKyl1pvr9tC2lI6Zz";
		public static final String CONSUMER_KEY = (DEV_MODE) ? CONSUMER_KEY_DEV : CONSUMER_KEY_PROD;
		
		public static final String CONSUMER_SECRET_DEV = "12er8G9HMElFmwR1YKyXLLN55vfeYrecRWgLZYnX";
		public static final String CONSUMER_SECRET_PROD = "yL0NNWXXz9ZZUwQlFhiuuLWKzrdjWu7H00KkXTrr";
		public static final String CONSUMER_SECRET = (DEV_MODE) ? CONSUMER_SECRET_DEV : CONSUMER_SECRET_PROD;
		
		public static class Urls {
			public static final String REQUEST_TOKEN_URL = OSM_API_URL + "/oauth/request_token";
			public static final String ACCESS_TOKEN_URL = OSM_API_URL + "/oauth/access_token";
			public static final String AUTHORIZE_TOKEN_URL = OSM_API_URL + "/oauth/authorize";
		}
			
	}
	
}
