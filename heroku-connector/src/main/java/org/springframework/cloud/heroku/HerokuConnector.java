package org.springframework.cloud.heroku;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.AbstractCloudConnector;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.FallbackServiceInfoCreator;
import org.springframework.cloud.app.ApplicationInstanceInfo;
import org.springframework.cloud.heroku.HerokuConnector.KeyValuePair;
import org.springframework.cloud.service.BaseServiceInfo;
import org.springframework.cloud.util.EnvironmentAccessor;

/**
 * Implementation of CloudConnector for Heroku
 * 
 * Currently support only the Postgres service.
 * 
 * @author Ramnivas Laddad
 *
 */
public class HerokuConnector extends AbstractCloudConnector<HerokuConnector.KeyValuePair> {

	private EnvironmentAccessor environment = new EnvironmentAccessor();
	private ApplicationInstanceInfoCreator applicationInstanceInfoCreator 
		= new ApplicationInstanceInfoCreator(environment);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HerokuConnector() {
		super((Class) HerokuServiceInfoCreator.class);
	}

	@Override
	public boolean isInMatchingCloud() {
		return environment.getEnvValue("DYNO") != null;
	}
	
	@Override
	public ApplicationInstanceInfo getApplicationInstanceInfo() {
		try {
			return applicationInstanceInfoCreator.createApplicationInstanceInfo();
		} catch (Exception e) {
			throw new CloudException(e);
		} 
	}
	
	/* package for testing purpose */
	void setCloudEnvironment(EnvironmentAccessor environment) {
		this.environment = environment;
		this.applicationInstanceInfoCreator = new ApplicationInstanceInfoCreator(environment);
	}

	/**
	 * Return object representation of the bound services
	 * <p>
	 * Returns map whose key is the env key and value is the associated url
	 * </p>
	 * @return
	 */
	protected List<KeyValuePair> getServicesData() {
		List<KeyValuePair> serviceData = new ArrayList<KeyValuePair>();
		
		Map<String,String> env = environment.getEnv();
		
		for (Map.Entry<String, String> envEntry : env.entrySet()) {
			if (envEntry.getKey().startsWith("HEROKU_POSTGRESQL_")) {
				serviceData.add(new KeyValuePair(envEntry.getKey(), envEntry.getValue()));
			}
		}

		return serviceData;
	}

	@Override
	protected FallbackServiceInfoCreator<BaseServiceInfo,KeyValuePair> getFallbackServiceInfoCreator() {
		return new HerokuFallbackServiceInfoCreator();
	}
	
	public static class KeyValuePair {
		private String key;
		private String value;

		public KeyValuePair(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}
	}
}

class HerokuFallbackServiceInfoCreator extends FallbackServiceInfoCreator<BaseServiceInfo,KeyValuePair> {
	@Override
	public BaseServiceInfo createServiceInfo(KeyValuePair serviceData) {
		return new BaseServiceInfo(serviceData.getKey());
	}
}
