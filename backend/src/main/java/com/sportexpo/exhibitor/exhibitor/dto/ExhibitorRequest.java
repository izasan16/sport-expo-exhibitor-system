package com.sportexpo.exhibitor.exhibitor.dto;

public record ExhibitorRequest(String companyName, String creditCode, String contactName,
                               String contactPhone, String contactEmail, String address, String status) {
}
