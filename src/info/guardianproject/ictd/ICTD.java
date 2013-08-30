package info.guardianproject.ictd;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPPublicKeyRingCollection;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.util.encoders.Hex;
import org.witness.informacam.Model;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

public class ICTD extends Model implements Serializable {
	private static final long serialVersionUID = -4806933659306611747L;
	
	public String organizationName = null;
	public String organizationDetails = null;
	public String organizationFingerprint = null;

	public byte[] publicKey = null;

	public List<IRepository> repositories = new ArrayList<IRepository>();
	public List<String> forms = new ArrayList<String>();

	private Context c;
	private final static String LOG = "*********************** ICTD Utility ***********************";
	private final static String APP = "application";

	public ICTD(Context c) {
		this.c = c;
		
		IRepository selfRepo = new IRepository();
		selfRepo.source = APP;
		selfRepo.packageName = this.c.getPackageName();
		
		try {
			for(Signature s : this.c.getPackageManager().getPackageInfo(this.c.getPackageName(), PackageManager.GET_SIGNATURES).signatures) {
				InputStream bais = new ByteArrayInputStream(s.toByteArray());
				CertificateFactory certFactory = null;
				X509Certificate cert = null;
				
				try {
					certFactory = CertificateFactory.getInstance("X509");
					cert = (X509Certificate) certFactory.generateCertificate(bais);
					
					MessageDigest md = MessageDigest.getInstance("SHA-1");
					md.update(cert.getEncoded());
					
					selfRepo.applicationSignature = new String(Hex.encode(md.digest()));
				} catch(CertificateException e) {
					Log.e(LOG, e.toString());
					continue;
				} catch (NoSuchAlgorithmException e) {
					Log.e(LOG, e.toString());
					e.printStackTrace();
					break;
				}
			}
			
			addRespository(selfRepo);
			Log.d(LOG, String.format("THIS ICTD:\n%s", this.asJson().toString()));
		} catch (NameNotFoundException e) {
			Log.e(LOG, e.toString());
			e.printStackTrace();
		}
	}
	
	private static String getFingerprint(byte[] keyBlock) throws IOException, PGPException {
		PGPPublicKeyRingCollection keyringCol = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(new ByteArrayInputStream(Base64.decode(keyBlock, Base64.DEFAULT))));
		PGPPublicKey key = null;
		Iterator<PGPPublicKeyRing> rIt = keyringCol.getKeyRings();
		while(key == null && rIt.hasNext()) {
			PGPPublicKeyRing keyring = (PGPPublicKeyRing) rIt.next();
			Iterator<PGPPublicKey> kIt = keyring.getPublicKeys();
			while(key == null && kIt.hasNext()) {
				PGPPublicKey k = (PGPPublicKey) kIt.next();
				if(k.isEncryptionKey())
					key = k;
			}
		}
		
		if(key == null) {
			return null;
		}

		return new String(Hex.encode(key.getFingerprint()));
		
	}

	public void setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
	}

	public void setOrganizationDetails(String organizationDetails) {
		this.organizationDetails = organizationDetails;
	}

	public void setPublicKey(byte[] publicKey) {
		try {
			organizationFingerprint = getFingerprint(this.publicKey);
			if(organizationFingerprint != null) {
				// TODO: GZIPPED?
				this.publicKey = Base64.encode(publicKey, Base64.DEFAULT);
			}
		} catch (IOException e) {
			Log.e(LOG, e.toString());
			e.printStackTrace();
		} catch (PGPException e) {
			Log.e(LOG, e.toString());
			e.printStackTrace();
		}
		
		publicKey = null;
	}
	
	public void setPublicKey(String publicKeyPath) {
		try {
			FileInputStream fis = c.openFileInput(publicKeyPath);
			if(fis.available() > 0) {
				byte[] buf = new byte[fis.available()];
				fis.read(buf);
				setPublicKey(buf);
			}
		} catch (FileNotFoundException e) {
			Log.e(LOG, e.toString());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(LOG, e.toString());
			e.printStackTrace();
		}
		
	}
	
	public void addRespository(IRepository repository) {
		if(!repositories.contains(repository)) {
			if(repository.source.equals(APP)) {
				for(IRepository r : repositories) {
					if(r.source.equals(APP)) {
						// FAIL.  only one app type allowed.
						return;
					}
				}
			}
			
			repositories.add(repository);
		}
	}
	
	public void addForm(String form) {
		// TODO: GZIPPED?
		form = Base64.encodeToString(form.getBytes(), Base64.DEFAULT);
		
		if(!forms.contains(form)) {
			forms.add(form);
		}
	}

	public class IRepository extends Model implements Serializable {
		private static final long serialVersionUID = 6778915736590912404L;

		public String source = APP;
		
		// apk sig (organization signing key) and package name
		public String applicationSignature = null;
		public String packageName = null;
		
		public String asset_id = null;
		public String asset_root = null;

		public IRepository() {
			super();
		}
	}
}
