import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

/**
 * One-time migration: populate the `slugs/{slug}` collection for all
 * existing pages that don't yet have a slug entry.
 *
 * Trigger via HTTP GET (admin use only — protect with a secret key param).
 * Example: GET https://.../migrateSlugs?key=<MIGRATION_KEY>
 */
export const migrateSlugs = functions.https.onRequest(async (req, res) => {
  // Basic auth guard — set MIGRATION_KEY env var before running
  const migrationKey = process.env.MIGRATION_KEY || "";
  if (!migrationKey || req.query.key !== migrationKey) {
    res.status(401).send("Unauthorized");
    return;
  }

  let profilesProcessed = 0;
  let slugsCreated = 0;
  let slugsSkipped = 0;
  let errors = 0;

  try {
    const profilesSnapshot = await db.collection("profiles").get();

    for (const profileDoc of profilesSnapshot.docs) {
      const username = profileDoc.id;
      const uid = profileDoc.data()?.uid as string | undefined;
      if (!uid) continue;

      profilesProcessed++;

      const pagesSnapshot = await db
        .collection("profiles")
        .doc(username)
        .collection("pages")
        .get();

      for (const pageDoc of pagesSnapshot.docs) {
        const slug = pageDoc.data()?.slug as string | undefined;
        if (!slug) continue;

        // Skip if slug already exists in registry
        const existing = await db.collection("slugs").doc(slug).get();
        if (existing.exists) {
          slugsSkipped++;
          continue;
        }

        try {
          await db.collection("slugs").doc(slug).set({
            username,
            pageId: pageDoc.id,
            uid,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });
          slugsCreated++;
        } catch (e) {
          errors++;
        }
      }
    }

    res.status(200).json({
      success: true,
      profilesProcessed,
      slugsCreated,
      slugsSkipped,
      errors,
    });
  } catch (e) {
    res.status(500).json({ success: false, error: String(e) });
  }
});
