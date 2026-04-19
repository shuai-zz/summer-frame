package org.example.with.tx;

import com.itranswarp.summer.annotation.Autowired;
import com.itranswarp.summer.annotation.Component;
import org.example.JdbcTestBase;
import org.example.annotation.Transactional;
import org.example.jdbc.JdbcTemplate;

import java.util.List;

@Component
@Transactional
public class AddressService {
    @Autowired
    UserService userService;
    @Autowired
    JdbcTemplate jdbcTemplate;

    public void addAddress(Address... addresses){
        for (Address address : addresses) {
            // check if userId is existed
            userService.getUser(address.userId);
            jdbcTemplate.update(JdbcTestBase.INSERT_ADDRESS, address.userId, address.address, address.zip);
        }
    }

    public List<Address> getAddresses(long userId){
        return jdbcTemplate.queryForList(JdbcTestBase.SELECT_ADDRESS_BY_USERID, Address.class, userId);
    }

    public void deleteAddress(long userId){
        jdbcTemplate.update(JdbcTestBase.DELETE_ADDRESS_BY_USERID, userId);
        if(userId==1){
            throw new RuntimeException("Rollback delete for user id = 1");
        }
    }
}
