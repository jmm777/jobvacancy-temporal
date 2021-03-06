package com.jobvacancy.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.jobvacancy.domain.Company;
import com.jobvacancy.domain.User;
import com.jobvacancy.repository.CompanyRepository;
import com.jobvacancy.repository.UserRepository;
import com.jobvacancy.security.SecurityUtils;
import com.jobvacancy.web.rest.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Company.
 */
@RestController
@RequestMapping("/api")
public class CompanyResource {

    private final Logger log = LoggerFactory.getLogger(CompanyResource.class);

    @Inject
    private CompanyRepository companyRepository;

    @Inject
    private UserRepository userRepository;

    /**
     * POST  /companys -> Create a new company.
     */
    @RequestMapping(value = "/companys",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Company> createCompany(@RequestBody Company company) throws URISyntaxException {
        log.debug("REST request to save Company : {}", company);
        if (company.getId() != null) {
            return ResponseEntity.badRequest().header("Failure", "A new company cannot already have an ID").body(null);
        }

        Company result = companyRepository.save(company);
        String currentLogin = SecurityUtils.getCurrentUserLogin();
        Optional<User> currentUser =  userRepository.findOneByLogin(currentLogin);
        currentUser.get().setCompany(company);
        userRepository.save(currentUser.get());
        return ResponseEntity.created(new URI("/api/companys/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("company", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /companys -> Updates an existing company.
     */
    @RequestMapping(value = "/companys",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Company> updateCompany(@RequestBody Company company) throws URISyntaxException {
        log.debug("REST request to update Company : {}", company);
        if (company.getId() == null) {
            return createCompany(company);
        }
        Company result = companyRepository.save(company);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("company", company.getId().toString()))
            .body(result);
    }

    /**
     * GET  /companys -> get all the companys.
     */
    @RequestMapping(value = "/companys",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<Company> getAllCompanys() {
        log.debug("REST request to get all Companys");
        String currentLogin = SecurityUtils.getCurrentUserLogin();
        Optional<User> currentUser =  userRepository.findOneByLogin(currentLogin);
        List<Company> companies = new ArrayList<Company>();
        Company company = currentUser.get().getCompany();
        if (company != null)
            companies.add(company);
        return companies;

    }

    /**
     * GET  /companys/:id -> get the "id" company.
     */
    @RequestMapping(value = "/companys/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Company> getCompany(@PathVariable Long id) {
        log.debug("REST request to get Company : {}", id);
        return Optional.ofNullable(companyRepository.findOne(id))
            .map(company -> new ResponseEntity<>(
                company,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /companys/:id -> delete the "id" company.
     */
    @RequestMapping(value = "/companys/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        log.debug("REST request to delete Company : {}", id);
        companyRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("company", id.toString())).build();
    }
}
