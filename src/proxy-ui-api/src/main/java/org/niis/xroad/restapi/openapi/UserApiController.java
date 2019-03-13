/**
 * The MIT License
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.restapi.openapi;

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.domain.ApiKeyType;
import org.niis.xroad.restapi.domain.Role;
import org.niis.xroad.restapi.openapi.model.User;
import org.niis.xroad.restapi.repository.ApiKey2Repository;
import org.niis.xroad.restapi.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * User controller
 */
@Controller
@RequestMapping("/api")
@Slf4j
@PreAuthorize("denyAll")
@Transactional
public class UserApiController implements org.niis.xroad.restapi.openapi.UserApi {

    private final NativeWebRequest request;

    @org.springframework.beans.factory.annotation.Autowired
    public UserApiController(NativeWebRequest request) {
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    /**
     * Return user object
     * @return
     */
    @PreAuthorize("permitAll()")
    @Override
    public ResponseEntity<User> user() {
        User user = new User();
        user.setUsername("username is unknown (TODO)");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        user.setPermissions(new ArrayList<>(getAuthorities(authentication, name -> !name.startsWith("ROLE_"))));
        user.setRoles(new ArrayList<>(getAuthorities(authentication, name -> name.startsWith("ROLE_"))));
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    /**
     * get roles
     * @param authentication
     * @return
     */
    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/roles")
    public ResponseEntity<Set<String>> getRoles(Authentication authentication) {
        return new ResponseEntity<>(
                getAuthorities(authentication, name -> name.startsWith("ROLE_")),
                HttpStatus.OK);
    }

    /**
     * get permissions
     * @param authentication
     * @return
     */
    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/permissions")
    public ResponseEntity<Set<String>> getPermissions(Authentication authentication) {
        return new ResponseEntity<>(
                getAuthorities(authentication, name -> !name.startsWith("ROLE_")),
                HttpStatus.OK);
    }

    private Set<String> getAuthorities(Authentication authentication,
                                       Predicate<String> authorityNamePredicate) {
        Set<String> roles = authentication.getAuthorities().stream()
                .map(authority -> ((GrantedAuthority) authority).getAuthority())
                .filter(authorityNamePredicate)
                .collect(Collectors.toSet());
        return roles;
    }

    /**
     * create new api key and store it - just development time, remove
     */
    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/key/insert")
    public ResponseEntity<String> insert() {
        String key = apiKeyRepository.create(Arrays.asList(
                Role.XROAD_REGISTRATION_OFFICER.name(),
                Role.XROAD_SERVICE_ADMINISTRATOR.name()));
        ApiKeyType apiKeyType = apiKeyRepository.get(key);
        apiKey2Repository.insertApiKey(apiKeyType);
        return new ResponseEntity<>(key,
                HttpStatus.OK);
    }

    /**
     * list api keys from db - just development time, remove
     */
    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/key/list")
    public ResponseEntity<Collection<ApiKeyType>> list() {
        Collection<ApiKeyType> keys = apiKey2Repository.getApiKeys();
        return new ResponseEntity<>(keys,
                HttpStatus.OK);
    }

    @Autowired
    private ApiKey2Repository apiKey2Repository;
    @Autowired
    private ApiKeyRepository apiKeyRepository;

}
