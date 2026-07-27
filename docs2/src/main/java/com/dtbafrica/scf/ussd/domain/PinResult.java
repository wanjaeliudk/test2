package com.dtbafrica.scf.ussd.domain;

/** LOCKED exists because Profile owns the attempt counter and lockout. */
public enum PinResult { OK, WRONG, LOCKED }
