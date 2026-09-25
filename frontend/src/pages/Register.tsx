import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { extractErrorMessage } from "../api/auth";
import FormField from "../components/FormField";

interface FieldErrors {
  name?: string;
  email?: string;
  password?: string;
}

const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

function validateEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  function validate(): boolean {
    const errors: FieldErrors = {};
    if (!name.trim()) errors.name = "Name is required";
    else if (name.trim().length < 2) errors.name = "Name must be at least 2 characters";

    if (!email.trim()) errors.email = "Email is required";
    else if (!validateEmail(email)) errors.email = "Enter a valid email address";

    if (!password) errors.password = "Password is required";
    else if (!PASSWORD_PATTERN.test(password)) {
      errors.password = "At least 8 characters, with a letter and a number";
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      await register({ name: name.trim(), email: email.trim(), password });
      navigate("/dashboard", { replace: true });
    } catch (err) {
      const message = extractErrorMessage(err);
      // Surface a duplicate-email conflict as a field error instead of a
      // generic banner, since it points to exactly one input.
      if (message.toLowerCase().includes("already exists")) {
        setFieldErrors((prev) => ({ ...prev, email: message }));
      } else {
        setFormError(message);
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen bg-paper flex items-center justify-center px-6 py-10">
      <div className="w-full max-w-sm">
        <Link to="/" className="flex items-center justify-center gap-2.5 mb-8">
          <span className="w-8 h-8 rounded-full bg-mint-600 flex items-center justify-center">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M4 4.5C4 3.12 5.79 2 8 2s4 1.12 4 2.5S10.21 7 8 7 4 5.88 4 4.5Z" stroke="white" strokeWidth="1.3" />
              <path d="M4 11.5C4 10.12 5.79 9 8 9s4 1.12 4 2.5S10.21 14 8 14s-4-1.12-4-2.5Z" stroke="white" strokeWidth="1.3" />
            </svg>
          </span>
          <span className="text-[17px] font-semibold tracking-tight text-ink">
            SplitWise<span className="text-mint-600">X</span>
          </span>
        </Link>

        <div className="bg-white rounded-xl2 border border-line shadow-card p-8">
          <h1 className="font-display text-2xl text-ink mb-1">Create your account</h1>
          <p className="text-[14px] text-muted mb-6">Start splitting expenses in a couple of minutes.</p>

          {formError && (
            <div className="mb-5 rounded-lg bg-rose-50 border border-rose-200 px-3.5 py-2.5 text-[13px] text-rose-700">
              {formError}
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <FormField
              id="name"
              label="Full name"
              type="text"
              autoComplete="name"
              placeholder="Sameer Kumar"
              value={name}
              onChange={(e) => setName(e.target.value)}
              error={fieldErrors.name}
            />
            <FormField
              id="email"
              label="Email"
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={fieldErrors.email}
            />
            <div>
              <FormField
                id="password"
                label="Password"
                type="password"
                autoComplete="new-password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                error={fieldErrors.password}
              />
              {!fieldErrors.password && (
                <p className="mt-1.5 text-[12px] text-muted">
                  At least 8 characters, with a letter and a number
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-full bg-mint-600 text-white text-[15px] font-medium py-2.5 hover:bg-mint-700 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {isSubmitting ? "Creating account…" : "Create account"}
            </button>
          </form>
        </div>

        <p className="text-center text-[14px] text-muted mt-6">
          Already have an account?{" "}
          <Link to="/login" className="text-mint-700 font-medium hover:text-mint-600">
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
}
