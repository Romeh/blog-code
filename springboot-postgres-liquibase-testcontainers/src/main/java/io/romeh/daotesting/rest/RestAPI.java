package io.romeh.daotesting.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.romeh.daotesting.rest.dto.CustomerDto;
import io.romeh.daotesting.rest.dto.CustomerMapper;
import io.romeh.daotesting.rest.dto.ErrorResponse;
import io.romeh.daotesting.service.CustomerService;

/**
 * the main rest api for customer CRUD
 */
@org.springframework.web.bind.annotation.RestController
@RequestMapping(value = "/customers", produces = MediaType.APPLICATION_JSON_VALUE)
public class RestAPI {

	private final CustomerService customerService;
	private final CustomerMapper customerMapper;

	@Autowired
	public RestAPI(CustomerService customerService, CustomerMapper customerMapper) {
		this.customerService = customerService;
		this.customerMapper = customerMapper;
	}

	@GetMapping(value = "/{id}")
	public CustomerDto getCustomerById(@PathVariable long id) {
		return customerMapper.mapCustomerToDto(customerService.findCustomerById(id));

	}

	@GetMapping(value = "/names/{name}")
	public CustomerDto getCustomerByName(@PathVariable String name) {
		return customerMapper.mapCustomerToDto(customerService.findCustomerByName(name));
	}

	@PostMapping
	public void createCustomer(@Valid @RequestBody CustomerDto customerDto) {
		customerService.createCustomer(customerMapper.mapDtoToCustomer(customerDto));
	}

	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleException(MethodArgumentNotValidException exception) {

		String errorMsg = exception.getBindingResult().getFieldErrors().stream()
				.map(DefaultMessageSourceResolvable::getDefaultMessage)
				.findFirst()
				.orElse(exception.getMessage());

		return ErrorResponse.builder().message(errorMsg).build();
	}

	@ExceptionHandler
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ErrorResponse handleException2(IllegalStateException exception) {

		return ErrorResponse.builder().message(exception.getLocalizedMessage()).build();
	}
}
