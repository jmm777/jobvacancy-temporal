package com.jobvacancy.web.rest;

import com.jobvacancy.Application;
import com.jobvacancy.domain.Company;
import com.jobvacancy.domain.User;
import com.jobvacancy.repository.CompanyRepository;

import com.jobvacancy.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.Matchers.hasItem;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for the CompanyResource REST controller.
 *
 * @see CompanyResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest
public class CompanyResourceTest {

    private static final String DEFAULT_NAME = "AAAAA";
    private static final String UPDATED_NAME = "BBBBB";

    private static final Integer DEFAULT_REPUTATION = 1;
    private static final Integer UPDATED_REPUTATION = 2;

    private static final LocalDate DEFAULT_REGISTRATION_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_REGISTRATION_DATE = LocalDate.now(ZoneId.systemDefault());

    @Inject
    private WebApplicationContext webApplicationContext;

    @Inject
    private UserRepository userRepository;

    @Inject
    private CompanyRepository companyRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restCompanyMockMvc;

    private Company company;

    private Optional<User> user;

    @Mock
    private UserRepository mockUserRepository;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        CompanyResource companyResource = new CompanyResource();
        ReflectionTestUtils.setField(companyResource, "companyRepository", companyRepository);

        user =  userRepository.findOneByLogin("user");
        user.get().setCompany(company);
        when(mockUserRepository.findOneByLogin(Mockito.any())).thenReturn(user);
        ReflectionTestUtils.setField(companyResource, "userRepository", mockUserRepository);

        this.restCompanyMockMvc = MockMvcBuilders.standaloneSetup(companyResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        company = new Company();
        company.setName(DEFAULT_NAME);
        company.setReputation(DEFAULT_REPUTATION);
        company.setRegistrationDate(DEFAULT_REGISTRATION_DATE);
    }

    @Test
    @Transactional
    public void createCompany() throws Exception {
        int databaseSizeBeforeCreate = companyRepository.findAll().size();

        restCompanyMockMvc.perform(post("/api/companys")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(company)))
                .andExpect(status().isCreated());

        // Validate the Company in the database
        List<Company> companys = companyRepository.findAll();
        assertThat(companys).hasSize(databaseSizeBeforeCreate + 1);
        Company testCompany = companys.get(companys.size() - 1);
        assertThat(testCompany.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testCompany.getReputation()).isEqualTo(DEFAULT_REPUTATION);
        assertThat(testCompany.getRegistrationDate()).isEqualTo(DEFAULT_REGISTRATION_DATE);
    }

    @Test
    @Transactional
    public void getAllCompanysShouldReturnThoseOfTheUser() throws Exception {

        // get the companies of the user
        Optional<User> user= userRepository.findOneByLogin("user");
        Company companyOfTheUser = user.get().getCompany();

        // create a new company
        companyRepository.saveAndFlush(company);

        // Get all the companies
        restCompanyMockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        restCompanyMockMvc.perform(get("/api/companys").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.[*].id").value(hasItem(companyOfTheUser.getId().intValue())))
                .andExpect(jsonPath("$.[*].name").value(hasItem(companyOfTheUser.getName())));
    }


    @Test
    @Transactional
    public void getCompany() throws Exception {
        // Initialize the database
        companyRepository.saveAndFlush(company);

        // Get the company
        restCompanyMockMvc.perform(get("/api/companys/{id}", company.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(company.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.reputation").value(DEFAULT_REPUTATION))
            .andExpect(jsonPath("$.registrationDate").value(DEFAULT_REGISTRATION_DATE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingCompany() throws Exception {
        // Get the company
        restCompanyMockMvc.perform(get("/api/companys/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateCompany() throws Exception {
        // Initialize the database
        companyRepository.saveAndFlush(company);

		int databaseSizeBeforeUpdate = companyRepository.findAll().size();

        // Update the company
        company.setName(UPDATED_NAME);
        company.setReputation(UPDATED_REPUTATION);
        company.setRegistrationDate(UPDATED_REGISTRATION_DATE);

        restCompanyMockMvc.perform(put("/api/companys")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(company)))
                .andExpect(status().isOk());

        // Validate the Company in the database
        List<Company> companys = companyRepository.findAll();
        assertThat(companys).hasSize(databaseSizeBeforeUpdate);
        Company testCompany = companys.get(companys.size() - 1);
        assertThat(testCompany.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testCompany.getReputation()).isEqualTo(UPDATED_REPUTATION);
        assertThat(testCompany.getRegistrationDate()).isEqualTo(UPDATED_REGISTRATION_DATE);
    }

    @Test
    @Transactional
    public void deleteCompany() throws Exception {
        // Initialize the database
        companyRepository.saveAndFlush(company);

		int databaseSizeBeforeDelete = companyRepository.findAll().size();

        // Get the company
        restCompanyMockMvc.perform(delete("/api/companys/{id}", company.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Company> companys = companyRepository.findAll();
        assertThat(companys).hasSize(databaseSizeBeforeDelete - 1);
    }
}