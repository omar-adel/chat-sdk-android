package co.chatsdk.firebase;

import android.support.annotation.NonNull;

import co.chatsdk.core.base.BaseUploadHandler;
import co.chatsdk.core.defines.Debug;

import com.braunster.chatsdk.object.ChatError;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import co.chatsdk.core.types.Defines;
import co.chatsdk.core.types.FileUploadResult;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import timber.log.Timber;

/**
 * Created by Erk on 26.07.2016.
 */
public class FirebaseUploadHandler extends BaseUploadHandler {

    private static final String TAG = FirebaseUploadHandler.class.getSimpleName();
    private static final boolean DEBUG = Debug.BFirebaseUploadHandler;

    public Observable<FileUploadResult> uploadFile(final byte[] data, final String name, final String mimeType) {
        return Observable.create(new ObservableOnSubscribe<FileUploadResult>() {
            @Override
            public void subscribe(final ObservableEmitter<FileUploadResult> e) throws Exception {

                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl(Defines.FirebaseStoragePath);
                StorageReference filesRef = storageRef.child("files");
                final String fullName = getUUID() + "_" + name;
                StorageReference fileRef = filesRef.child(fullName);

                final FileUploadResult result = new FileUploadResult();

                UploadTask uploadTask = fileRef.putBytes(data);

                uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        result.progress.set(taskSnapshot.getTotalByteCount(), taskSnapshot.getBytesTransferred());

                        // TODO: With Firebase this appears to be broken
                        //e.onNext(result);
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        result.name = name;
                        result.mimeType = mimeType;
                        result.url = taskSnapshot.getDownloadUrl().toString();
                        result.progress.set(taskSnapshot.getTotalByteCount(), taskSnapshot.getTotalByteCount());
                        e.onNext(result);
                        e.onComplete();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception error) {
                        if (DEBUG) Timber.e(error.getCause(), "Firebase storage exception while saving");
                        e.onError(ChatError.getError(ChatError.Code.FIREBASE_STORAGE_EXCEPTION, error.getMessage()));
                    }
                });

            }
        });
    }



}