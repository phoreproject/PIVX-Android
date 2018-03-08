package io.phore.android.rate;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.phore.android.rate.db.PhoreRate;

/**
 * Created by furszy on 7/5/17.
 */

public class CoinMarketCapApiClient {

    private static final String URL = "https://api.coinmarketcap.com/v1/";

    public class PhoreMarket {
        public BigDecimal priceUsd;
        public BigDecimal priceBtc;
        public BigDecimal marketCapUsd;
        public BigDecimal totalSupply;
        public int rank;

        public PhoreMarket(BigDecimal priceUsd, BigDecimal priceBtc, BigDecimal marketCapUsd, BigDecimal totalSupply, int rank) {
            this.priceUsd = priceUsd;
            this.priceBtc = priceBtc;
            this.marketCapUsd = marketCapUsd;
            this.totalSupply = totalSupply;
            this.rank = rank;
        }
    }

    public PhoreMarket getPhorePrice() throws RequestPhoreRateException {
        try {
            PhoreMarket phoreMarket = null;
            String url = this.URL + "ticker/phore/";
            HttpResponse httpResponse = get(url);
            // receive response as inputStream
            InputStream inputStream = httpResponse.getEntity().getContent();
            String result = null;
            if (inputStream != null)
                result = convertInputStreamToString(inputStream);
            if (httpResponse.getStatusLine().getStatusCode()==200){
                JSONArray jsonArray = new JSONArray(result);
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                phoreMarket = new PhoreMarket(
                        new BigDecimal(jsonObject.getString("price_usd")),
                        new BigDecimal(jsonObject.getString("price_btc")),
                        new BigDecimal(jsonObject.getString("market_cap_usd")),
                        new BigDecimal(jsonObject.getString("total_supply")),
                        jsonObject.getInt("rank")
                );
            }
            return phoreMarket;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            throw new RequestPhoreRateException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RequestPhoreRateException(e);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RequestPhoreRateException(e);
        }
    }

    public static HttpResponse get(String url) throws IOException {
        BasicHttpParams basicHttpParams = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(basicHttpParams, (int) TimeUnit.MINUTES.toMillis(1));
        HttpClient client = new DefaultHttpClient(basicHttpParams);
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Accept", "text/html,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        httpGet.setHeader("Content-type", "application/json");
        return client.execute(httpGet);
    }


    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream,"ISO-8859-1"));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;
    }

    public static class BitPayApi {

        private final String URL = "https://bitpay.com/rates";

        public interface RatesConvertor<T>{

            T convertRate(String code, String name, BigDecimal bitcoinRate);

        }

        /**
         * {"code":"BTC","name":"Bitcoin","rate":1}
         * @return
         * @throws RequestPhoreRateException
         */
        public <T> List<T> getRates(RatesConvertor<T> ratesConvertor) throws RequestPhoreRateException{
            try {
                HttpResponse httpResponse = get(URL);
                // receive response as inputStream
                InputStream inputStream = httpResponse.getEntity().getContent();
                String result = null;
                List<T> ret = new ArrayList<>();
                if (inputStream != null)
                    result = convertInputStreamToString(inputStream);
                if (httpResponse.getStatusLine().getStatusCode()==200){
                    JSONArray jsonArray = new JSONObject(result).getJSONArray("data");
                    for (int i=0; i < jsonArray.length(); i++){
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String code = jsonObject.getString("code");
                        String name = jsonObject.getString("name");
                        BigDecimal rate = new BigDecimal(jsonObject.getString("rate"));
                        ret.add(ratesConvertor.convertRate(code,name,rate));
                    }
                }
                return ret;
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                throw new RequestPhoreRateException(e);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RequestPhoreRateException(e);
            } catch (JSONException e) {
                e.printStackTrace();
                throw new RequestPhoreRateException(e);
            }
        }

    }

}
