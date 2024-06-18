package io.forest.txnoutbox.common;

public enum ResponseStatus {

	OK;

	public boolean isOk() {
		return this.equals(OK);
	}
}
