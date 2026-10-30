import { DtCurrencyPipe } from './dt-currency.pipe';

describe('DtCurrencyPipe', () => {
  const pipe = new DtCurrencyPipe();

  it('formats all amounts with the fr-TN three-decimal DT format', () => {
    expect([56, 1012, 0, 250.5, -56].map((amount) => pipe.transform(amount))).toEqual([
      '56,000 DT',
      '1\u202f012,000 DT',
      '0,000 DT',
      '250,500 DT',
      '-56,000 DT',
    ]);
  });
});
