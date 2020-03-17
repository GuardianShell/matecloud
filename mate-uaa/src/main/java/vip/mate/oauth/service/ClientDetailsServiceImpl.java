package vip.mate.oauth.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vip.mate.common.constant.Oauth2Constant;

import javax.sql.DataSource;

/**
 * 自定义client表，并将数据缓存到redis，处理缓存优化
 * 需要在管理平台修改client数据时，同步至redis
 * @author xuzhanfu
 */

@Slf4j
@Setter
public class ClientDetailsServiceImpl extends JdbcClientDetailsService {

    private RedisTemplate<String, Object> redisTemplate;

    public ClientDetailsServiceImpl(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * 从redis里读取ClientDetails
     * @param clientId
     * @return
     * @throws InvalidClientException
     */
    @Override
    public ClientDetails loadClientByClientId(String clientId) throws InvalidClientException {
        ClientDetails clientDetails = (ClientDetails) redisTemplate.opsForValue().get(clientKey(clientId));
        if (StringUtils.isEmpty(clientDetails)){
            clientDetails = getCacheClient(clientId);
        }
        return clientDetails;
    }

    /**
     * 自定义语句查询，并将数据同步至redis
     * @param clientId
     * @return
     */
    private ClientDetails getCacheClient(String clientId) {
        ClientDetails clientDetails = null;

        try {
            clientDetails = super.loadClientByClientId(clientId);
            if (!StringUtils.isEmpty(clientDetails)){
                redisTemplate.opsForValue().set(clientKey(clientId), clientDetails);
                log.debug("Cache clientId:{}, clientDetails:{}", clientId, clientDetails);
            }
        } catch (Exception e){
            log.error("Exception for clientId:{}, message:{}", clientId, e.getMessage());
        }
        return clientDetails;
    }

    private String clientKey(String clientId) {
        return Oauth2Constant.CLIENT_TABLE + ":" + clientId;
    }
}
