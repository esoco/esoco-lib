//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2015 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package de.esoco.lib.datatype;

import java.math.BigDecimal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/********************************************************************
 * An enumeration of currencies.
 */
public enum Currency
{
	AED, ANG, AUD, BTN, CHF, CZK, DKK, EUR, GBP, HKD, HRK, HUF, IDR, ILS, KZT,
	LKR, NGN, NOK, PLN, RUB, RSD, THB, TRY, UAH, USD, JPY, ZAR;

	//~ Static fields/initializers ---------------------------------------------

	private static Map<Currency, BigDecimal> aEuroConversionRates =
		new HashMap<Currency, BigDecimal>();

	static
	{
		aEuroConversionRates.put(EUR, BigDecimal.ONE);

		// conversion rate on 26.08.2010 from google
		// AUD, GBP, JPY, KZT, NGN, NOK 31.03.2011
		// LKR 03.06.2011
		// BTN 13.07.2011
		// ILS 22.08.2011
		// HKD, USD 20.10.2011
		aEuroConversionRates.put(AED, new BigDecimal("0.214528049"));
		aEuroConversionRates.put(ANG, new BigDecimal("0.450227702"));
		aEuroConversionRates.put(AUD, new BigDecimal("0.72795101"));
		aEuroConversionRates.put(BTN, new BigDecimal("0.0159492"));
		aEuroConversionRates.put(CHF, new BigDecimal("0.767109203"));
		aEuroConversionRates.put(CZK, new BigDecimal("0.0401599433"));
		aEuroConversionRates.put(DKK, new BigDecimal("0.134268043"));
		aEuroConversionRates.put(GBP, new BigDecimal("1.13155487"));
		aEuroConversionRates.put(HKD, new BigDecimal("0.0938001752"));
		aEuroConversionRates.put(HRK, new BigDecimal("0.137403089"));
		aEuroConversionRates.put(HUF, new BigDecimal("0.00354238891"));
		aEuroConversionRates.put(IDR, new BigDecimal("0.0000874566656"));
		aEuroConversionRates.put(ILS, new BigDecimal("0.194063433"));
		aEuroConversionRates.put(JPY, new BigDecimal("0.00850285071"));
		aEuroConversionRates.put(KZT, new BigDecimal("0.00483423664"));
		aEuroConversionRates.put(LKR, new BigDecimal("0.00629468599"));
		aEuroConversionRates.put(NGN, new BigDecimal("0.00454142324"));
		aEuroConversionRates.put(NOK, new BigDecimal("0.127643415"));
		aEuroConversionRates.put(PLN, new BigDecimal("0.250356918"));
		aEuroConversionRates.put(RSD, new BigDecimal("0.009483"));
		aEuroConversionRates.put(RUB, new BigDecimal("0.0255601954"));
		aEuroConversionRates.put(THB, new BigDecimal("0.0250283643"));
		aEuroConversionRates.put(TRY, new BigDecimal("0.516417428"));
		aEuroConversionRates.put(UAH, new BigDecimal("0.0998479357"));
		aEuroConversionRates.put(USD, new BigDecimal("0.729820464"));
		aEuroConversionRates.put(ZAR, new BigDecimal("0.107166719"));
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Converts a value into a certain currency with the exchange rate on a
	 * certain date.
	 *
	 * @param  rCurrency The original currency of the value
	 * @param  rValue    The value to convert
	 * @param  rDate     The date to look up the exchange rate for
	 *
	 * @return The resulting BigDecimal value
	 */
	public BigDecimal convertTo(Currency   rCurrency,
								BigDecimal rValue,
								Date	   rDate)
	{
		if (rCurrency != EUR)
		{
			throw new UnsupportedOperationException("Only conversions to EURO are currently supported");
		}

		if (this == EUR)
		{
			return rValue;
		}
		else
		{
			assert aEuroConversionRates.get(this) != null;

			return rValue.multiply(aEuroConversionRates.get(this));
		}
	}
}
