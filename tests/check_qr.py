"""Cross-check the complete QR symbol against an independent implementation."""
import subprocess, sys, qrcode
from qrcode.util import QRData, MODE_8BIT_BYTE
urls = ['http://192.168.1.100:8765', 'http://10.0.0.2:8765', 'http://172.31.255.254:8765']
lines = subprocess.check_output(['java', '-cp', sys.argv[1], 'io.github.smileplzz.tvbridge.QrDump', *urls], text=True).splitlines()
for i, url in enumerate(urls):
    assert lines[i * 34] == url
    expected = qrcode.QRCode(version=4, error_correction=qrcode.constants.ERROR_CORRECT_L, border=0, mask_pattern=0)
    expected.add_data(QRData(url, mode=MODE_8BIT_BYTE), optimize=0)
    expected.make(fit=False)
    actual = lines[i * 34 + 1:i * 34 + 34]
    assert actual == [''.join(str(int(v)) for v in row) for row in expected.get_matrix()], f'QR mismatch: {url}'
print('3 QR matrices match independent python-qrcode implementation')
