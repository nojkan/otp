package controllers;

import play.*;

import play.mvc.*;
import play.mvc.Http.*;
import play.libs.F.Function;
import play.libs.F.Promise;

import views.html.*;

//utils
import java.util.Random;
import play.Logger;
import java.security.SecureRandom;
import javax.inject.Inject;

import java.util.regex.*;

//redis
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;
//ws
import play.libs.ws.*;
//Json
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;


public class Application extends Controller {

	@Inject 
	JedisPool jedisPool; 

	private Jedis myJedis;

    private Jedis getRedis(){

    	if(myJedis == null){
    		myJedis = jedisPool.getResource();
    	}
    	return myJedis;
    }

    public Result index() {
        return ok(index.render("Your new application is ready."));
    }


	//TODO response as recommendation

    //The JedisPool will be injected for you from the module
    
    public Result subscribe(String MSISDN){

    	//check if Number format is valid 
    	String regex = "^\\+(?:[0-9]?){12,14}[0-9]$";
		Pattern pattern = Pattern.compile(regex);

		Matcher matcher = pattern.matcher(MSISDN);

		if (matcher.matches() == false){
			return badRequest("wrong number format please refer to E.164 number formatting, ");
		} 

    	Random randomGenerator = new SecureRandom();
        
           int randomInt = randomGenerator.nextInt(10000);
           Logger.debug("Generated : " + randomInt);

         Jedis redis = getRedis();
         //set in redis database
         redis.set(MSISDN,""+randomInt);
         //setting the ttl in seconds (15 min)
         redis.expire(MSISDN, 900);

         Logger.debug("Saved in redis randomInt : " + randomInt);
        //play.cache.Cache.set(MSISDN,""+randomInt);
         
        
    	return ok("e-taxis code : "+randomInt + " valid for 15 minutes");
    }

   
    public Result check(String MSISDN, String token){
    	String savedToken = "";

    	Jedis redis = getRedis();
    	
    	savedToken = redis.get(MSISDN);
    	
    	Logger.debug("SavedToken for " + MSISDN + ":" + savedToken);

    	if (token.equals(savedToken)){
    		return ok("authorized");
    	} else {
    		return unauthorized("UnAuthorized");
    	}

    }

   
    	//https://www.typesafe.com/blog/play-framework-with-java-8
    @Inject WSClient ws;
    public Promise<Result> sms(){

        RequestBody body = request().body();
        JsonNode jsonBody = body.asJson();

    	WSRequest request = ws.url("https://api.plivo.com/v1/Account/MANDJLMJAYYTG2N2ZINZ/Message/");
    	request.setAuth("MANDJLMJAYYTG2N2ZINZ", "ZTZiMTgzZjMzYjFhMWY5YzAxNDc4NmViNTViNTg0", WSAuthScheme.BASIC);
        request.setHeader("Content-Type", "application/json");                              
        request.post(jsonBody);

        Promise<WSResponse> jsonPromise = request.post(jsonBody);

        return jsonPromise.map(response -> ok(response.asJson()));

    } 	

    //public JsonNode plivoSms(JsonNode jsonBody){

 //   	WSRequest request = ws.url("https://api.plivo.com/v1/Account/MANDJLMJAYYTG2N2ZINZ/Message/");
 //   	request.setAuth("MANDJLMJAYYTG2N2ZINZ", "ZTZiMTgzZjMzYjFhMWY5YzAxNDc4NmViNTViNTg0", WSAuthScheme.BASIC);
 //       request.setHeader("Content-Type", "application/json");                              
 //       request.post(jsonBody);

  //      Promise<WSResponse> jsonPromise = request.post(jsonBody);
  //      return jsonPromise.map(response -> ok(response.asJson()));
    //

    //}

}


