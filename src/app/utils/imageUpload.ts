import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { storage, auth } from "../config/firebase";
import { toast } from "sonner";

export async function uploadImage(file: File): Promise<string> {
  if (!file.type.startsWith("image/")) {
    throw new Error("File must be an image");
  }

  const maxSize = 5 * 1024 * 1024;
  if (file.size > maxSize) {
    throw new Error("Image must be less than 5MB");
  }

  const uid = auth.currentUser?.uid;
  if (!uid) {
    throw new Error("Must be authenticated to upload images");
  }

  const ext = file.name.split(".").pop() || "jpg";
  const filename = `${Date.now()}.${ext}`;
  const storageRef = ref(storage, `profile_images/${uid}/${filename}`);

  await uploadBytes(storageRef, file, { contentType: file.type });
  return getDownloadURL(storageRef);
}

export function createImageInput(onChange: (url: string) => void): void {
  const input = document.createElement("input");
  input.type = "file";
  input.accept = "image/*";
  input.onchange = async (e) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) {
      try {
        const url = await uploadImage(file);
        onChange(url);
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "Failed to upload image");
      }
    }
  };
  input.click();
}
