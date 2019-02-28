package io.romeh.daotesting.rest.dto;

import lombok.Builder;
import lombok.ToString;

/**
 * @author romeh
 */
@Builder
@ToString
public class ErrorResponse {

	private String message;
}
